package gg.hermes.engine;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.HermesProcess;
import gg.hermes.tasks.*;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParseException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class HermesConcurrentGraph implements HermesGraph
{
    private static class GraphNode {
        final ITask task;
        AbstractGraphArch[] arches;

        GraphNode(ITask task) {
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

    private class ConditionalGraphArch implements AbstractGraphArch {
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

        class ConditionArch {
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

    private final Logger log;
    private final GraphNode[] graph;
    private final Map<String, Map<String, Object>> graphVariables;
    private final Map<Integer, Lock> lockingPointers;
    private final JsonLogicEvaluator jsonLogicEvaluator;
    private volatile int isEnd;

    public HermesConcurrentGraph(HermesProcess hermesProcess, JsonLogicConfiguration jsonLogicConf, Logger log)
    {
        // complete validation of the process
        hermesProcess.validate();

        this.log = log;
        isEnd = 0;
        jsonLogicEvaluator = jsonLogicConf.getEvaluator();

        graph = new GraphNode[hermesProcess.getNodes().size()];

        final int howManyNodeWithVariables = createGraphNodes(hermesProcess);
        graphVariables = new HashMap<>(howManyNodeWithVariables);
        initializeGraphVariables();

        createArches(hermesProcess);

        lockingPointers = new ConcurrentHashMap<>();
        lockingPointers.put(hermesProcess.getStartingNodeId(), new ReentrantLock());
    }

    private int createGraphNodes(HermesProcess hermesProcess)
    {
        int howManyNodeWithVariables = 0;
        final int nodeListSize = hermesProcess.getNodes().size();
        for (int i = 0; i < nodeListSize; ++i)
        {
            var task = hermesProcess.getNodes().get(i);
            graph[i] = new GraphNode(TaskFactory.createNewTaskFrom(task, i, hermesProcess));
            if (graph[i].task.getNumberOfVariables() > 0)
                ++howManyNodeWithVariables;
        }
        return howManyNodeWithVariables;
    }

    private void initializeGraphVariables()
    {
        for (GraphNode node : graph) {
            if (node.task instanceof NormalTask nTask && nTask.getNumberOfVariables() > 0)
                graphVariables.put(nTask.getIdAsString(), new HashMap<>(nTask.getNumberOfVariables()));
        }
    }

    private static void addArch(final AbstractGraphArch[] arches, final AbstractGraphArch newArch) {
        final int size = arches.length;
        for (int i = 0; i < size; ++i) {
            if (arches[i] == null) {
                arches[i] = newArch;
                break;
            }
        }
    }

    private void createArches(final HermesProcess hermesProcess)
    {
        final int[] numberOfArchesForNode = new int[graph.length];
        for (final Arch arch : hermesProcess.getArches())
            ++numberOfArchesForNode[arch.src()];

        final int size = hermesProcess.getArches().size();
        int i = 0;
        try {
            for (; i < size; ++i) {
                final Arch arch = hermesProcess.getArches().get(i);
                final int sourceIdx = arch.src();
                if (graph[sourceIdx].arches == null) {
                    graph[sourceIdx].arches = new AbstractGraphArch[numberOfArchesForNode[sourceIdx]];
                }

                AbstractGraphArch newArch;
                List<Arch.ConditionArch> conditions;
                if ((conditions = arch.conditions()) != null) {
                    newArch = new ConditionalGraphArch(conditions.size());
                    ConditionalGraphArch cga = (ConditionalGraphArch) newArch;
                    for (var conditionArch : conditions) {
                        cga.addCondition(
                                conditionArch.dst(),
                                conditionArch.IF() != null ? JsonLogicParser.parse(conditionArch.IF()) : null
                        );
                    }
                } else {
                    newArch = new GraphArch(arch.dst());
                }

                addArch(graph[sourceIdx].arches, newArch);
            }
        } catch (JsonLogicParseException e) {
            log.error(e.getMessage());
            throw new IllegalHermesProcess("One of the Conditions in Arch " + i + " is not a valid Json Logic Expression.");
        }
    }

    /**
     * Returns the reference of currently active tasks to complete.
     * Never returns tasks of type {@code FORWARD}, {@code JOIN} and {@code ENDING}.
     * @return reference of currently active tasks.
     */
    @Override
    public List<ITask> getCurrentTasks()
    {
        if (isEnd < 0)
            return Collections.emptyList();

        final List<ITask> tasks = new ArrayList<>(lockingPointers.size());
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

        final Lock lock = lockingPointers.get(currentNodeId);
        if (lock == null)
            return LOCK_REJECTED;

        final GraphNode currentNode = graph[currentNodeId];
        Map<String, Object> currentNodeVariables = null;

        int nv;
        if (currentNode.task instanceof NormalTask nTask
            && (nv = nTask.getNumberOfVariables()) > 0
            && variables != null && variables.size() <= nv
        ) {
            currentNodeVariables = graphVariables.get(nTask.getIdAsString());
        }

        if (! lock.tryLock())
            return LOCK_REJECTED;
        try {
            if (currentNodeVariables != null) {
                currentNodeVariables.clear();
                currentNodeVariables.putAll(variables);
            }
            return completeMovement(currentNodeId);
        }
        finally {
            lock.unlock();
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
        final Queue<GraphNode> nodes = new ArrayDeque<>();

        move(pointerKey, nodes);
        while ((node = nodes.poll()) != null) {
            final ITask task = node.task;
            next = task.getId();

            switch (task.getType()) {
                case NORMAL:
                    if (finalResult == SUCCESS && next != pointerKey) {
                        if (lockReused)
                            lockingPointers.putIfAbsent(next, new ReentrantLock());
                        else {
                            lockingPointers.putIfAbsent(next, lockingPointers.remove(pointerKey));
                            lockReused = true;
                        }
                    }
                    break;

                case JOIN:
                    final JoinTask joinTask = (JoinTask)task;
                    if (joinTask.safeIncrement()) {
                        move(next, nodes);
                        joinTask.safeReset();
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
                    String error = "Invalid Task Type at run time.";
                    log.error(error);
                    throw new IllegalHermesProcess(error);
            }
        }
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

