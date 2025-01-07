package gg.hermes.engine;

import gg.hermes.Arch;
import gg.hermes.IllegalHermesProcess;
import gg.hermes.HermesProcess;
import gg.hermes.nodes.*;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParseException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class HermesConcurrentGraph implements HermesGraph
{
    private static final class GraphNode {
        final HermesTask task;
        AbstractGraphArch[] arches;

        GraphNode(HermesTask task) {
            this.task = task;
            arches = null;
        }
    }

    private interface AbstractGraphArch {
        int evaluate();
    }

    private record GraphArch(int destination) implements AbstractGraphArch {
        public int evaluate() {
            return destination;
        }
    }

    private final class ConditionalGraphArch implements AbstractGraphArch {
        final List<ConditionArch> conditions;

        ConditionalGraphArch(int conditionListSize) {
            this.conditions = new ArrayList<>(conditionListSize);
        }

        void addCondition(int destination, JsonLogicNode condition) {
            conditions.add(new ConditionArch(destination, condition));
        }

        public int evaluate() {
            for (var condition : conditions) {
                if (condition.evaluate())
                    return condition.destination;
            }
            return -1;
        }

        final class ConditionArch {
            final int destination;
            final JsonLogicNode condition;

            ConditionArch(int destination, JsonLogicNode condition) {
                this.destination = destination;
                this.condition = condition;
            }

            boolean evaluate() {
                if (condition == null)
                    return true;
                try {
                    return (boolean) jsonLogicEvaluator.evaluate(condition, graphVariables);
                }
                catch (JsonLogicException | ClassCastException e) {
                    log.error(e.getMessage());
                    throw new IllegalHermesProcess(
                            String.format("Condition of Arch to node %d is not a correct Json Logic Expression.", destination));
                }
            }
        }
    }

    private static final int TRUE = 1;
    private static final int FALSE = 0;

    private final Logger log;
    private final GraphNode[] graph;
    private final Map<String, Map<String, Object>> graphVariables;
    private final Map<Integer, AtomicInteger> lockingPointers;
    private final JsonLogicEvaluator jsonLogicEvaluator;
    private volatile int isEnd;

    public HermesConcurrentGraph(HermesProcess hermesProcess, JsonLogicConfiguration jsonLogicConf, Logger log)
    {
        // complete validation of the process
        hermesProcess.validate();

        this.log = log;
        isEnd = 0;
        jsonLogicEvaluator = jsonLogicConf.getEvaluator();

        graph = new GraphNode[hermesProcess.getProcess().size()];

        final int howManyNodeWithVariables = createGraphNodes(hermesProcess);
        graphVariables = new HashMap<>(howManyNodeWithVariables);
        initializeGraphVariables();

        createArches(hermesProcess);

        lockingPointers = new ConcurrentHashMap<>();
        lockingPointers.put(0, new AtomicInteger(FALSE));
    }

    private int createGraphNodes(HermesProcess hermesProcess)
    {
        int howManyNodeWithVariables = 0;
        final int nodeListSize = hermesProcess.getProcess().size();
        for (int i = 0; i < nodeListSize; ++i)
        {
            var task = hermesProcess.getProcess().get(i);
            graph[i] = new GraphNode(HermesNodeFactory.createNewTaskFrom(task, i, hermesProcess));
            if (graph[i].task.getNumberOfVariables() > 0)
                ++howManyNodeWithVariables;
            if (task.getTo() != null)
                graph[i].arches = new AbstractGraphArch[task.getTo().size()];
        }
        return howManyNodeWithVariables;
    }

    private void initializeGraphVariables()
    {
        for (GraphNode node : graph) {
            if (node.task instanceof Task nTask && nTask.getNumberOfVariables() > 0)
                graphVariables.put(nTask.getIdAsString(), new HashMap<>(nTask.getNumberOfVariables()));
        }
    }

    private ConditionalGraphArch createConditionalArch(final List<Arch.ConditionArch> conditions)
    {
        try {
            final ConditionalGraphArch cga = new ConditionalGraphArch(conditions.size());
            for (var conditionArch : conditions) {
                cga.addCondition(
                        conditionArch.dst(),
                        conditionArch.IF() != null ? JsonLogicParser.parse(conditionArch.IF()) : null
                );
            }
            return cga;
        } catch (JsonLogicParseException e) {
            log.error(e.getMessage());
            throw new IllegalHermesProcess("One of the Conditions is not a valid Json Logic Expression.");
        }
    }

    private void createArches(final HermesProcess hermesProcess)
    {
        final int size = graph.length;
        final List<HermesNode> process = hermesProcess.getProcess();
        for (int i = 0; i < size; ++i) {
            var arches = process.get(i).getTo();
            if (arches == null)
                continue;

            int j = 0;
            for (var arch : arches) {
                graph[i].arches[j] = (arch.conditions() != null)
                        ? createConditionalArch(arch.conditions())
                        : new GraphArch(arch.dst());
                ++j;
            }
        }
    }

    /**
     * Returns the reference of currently active tasks to complete.
     * Never returns tasks of type {@code FORWARD}, {@code JOIN} and {@code ENDING}.
     * @return reference of currently active tasks.
     */
    @Override
    public List<HermesTask> getCurrentTasks()
    {
        if (isEnd < 0)
            return Collections.emptyList();

        final List<HermesTask> tasks = new ArrayList<>(lockingPointers.size());
        for (int ptr : lockingPointers.keySet())
            tasks.add(graph[ptr].task);
        return tasks;
    }

    @Override
    public int completeTask(final int currentNodeId) {
        return completeTask(currentNodeId, null);
    }

    @Override
    public int completeTask(final int currentNodeId, final Map<String, Object> variables)
    {
        if (isEnd < 0) return isEnd;

        final AtomicInteger lock = lockingPointers.get(currentNodeId);
        if (lock == null)
            return LOCK_REJECTED;

        final GraphNode currentNode = graph[currentNodeId];
        Map<String, Object> currentNodeVariables = null;

        int nv;
        if (currentNode.task instanceof Task nTask
            && (nv = nTask.getNumberOfVariables()) > 0
            && variables != null && variables.size() <= nv
        ) {
            currentNodeVariables = graphVariables.get(nTask.getIdAsString());
        }

        if (! lock.compareAndSet(FALSE, TRUE))
            return LOCK_REJECTED;
        try {
            if (currentNodeVariables != null) {
                currentNodeVariables.clear();
                currentNodeVariables.putAll(variables);
            }
            return completeMovement(currentNodeId);
        }
        finally {
            lock.set(FALSE);
        }
    }

    /**
     Logic:
     1. If no ENDING is found, the result is SUCCESS, otherwise it is the ENDING found;
     2. In case of multiple ENDINGS, the first one found takes precedence;
     3. If the process is not finished then the lock of the current pointer is reused.
     */
    private int completeMovement(final int pointerKey) {
        int next;
        GraphNode node;
        int finalResult = SUCCESS;
        boolean lockReused = false;
        boolean isSelfLoop = false;
        final Queue<GraphNode> nodes = new ArrayDeque<>();

        move(pointerKey, nodes);
        while ((node = nodes.poll()) != null) {
            final HermesTask task = node.task;
            next = task.getId();

            switch (task.getType()) {
                case NORMAL:
                    if (finalResult == SUCCESS) {
                        if (next != pointerKey) {
                            if (lockReused)
                                lockingPointers.putIfAbsent(next, new AtomicInteger(FALSE));
                            else {
                                lockingPointers.putIfAbsent(next, lockingPointers.remove(pointerKey));
                                lockReused = true;
                            }
                        } else if (nodes.isEmpty())
                            isSelfLoop = true;
                    }
                    break;

                case JOIN:
                    final Join join = (Join)task;
                    if (join.safeIncrement()) {
                        move(next, nodes);
                        join.safeReset();
                    }
                    break;

                case FORWARD:
                    move(next, nodes);
                    break;

                case ENDING:
                    if (finalResult == SUCCESS)
                        finalResult = isEnd = task.isGoodEnding() ? GOOD_ENDING : BAD_ENDING;
                    break;

                default:
                    String error = "Invalid Node Type at run time.";
                    log.error(error);
                    throw new IllegalHermesProcess(error);
            }
        }
        if (! isSelfLoop)
            lockingPointers.remove(pointerKey);
        return (lockingPointers.isEmpty() && finalResult == SUCCESS) ? STALEMATE_ENDING : finalResult;
    }

    private void move(final int from, final Queue<GraphNode> pointers) {
        int destination;
        for (final AbstractGraphArch arch : graph[from].arches) {
            if ((destination = arch.evaluate()) != -1)
                pointers.add(graph[destination]);
        }
    }
}

