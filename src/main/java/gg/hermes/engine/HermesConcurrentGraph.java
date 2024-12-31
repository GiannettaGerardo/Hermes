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
        List<AbstractGraphArch> arches;

        GraphNode(ITask task) {
            this.task = task;
            arches = null;
        }

        boolean isEnding() {
            return TaskType.ENDING == task.getType();
        }
    }

    private abstract static class AbstractGraphArch {
        final int source;

        AbstractGraphArch(int source) {
            this.source = source;
        }

        abstract int evaluate();
    }

    private static class GraphArch extends AbstractGraphArch {
        final int destination;

        GraphArch(int source, int destination) {
            super(source);
            this.destination = destination;
        }

        int evaluate() {
            return destination;
        }
    }

    private class ConditionalGraphArch extends AbstractGraphArch {
        final List<ConditionArch> conditions;

        ConditionalGraphArch(int source, int conditionListSize) {
            super(source);
            this.conditions = new ArrayList<>(conditionListSize);
        }

        void addCondition(int destination, JsonLogicNode condition) {
            conditions.add(new ConditionArch(destination, condition));
        }

        int evaluate() {
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
                            String.format("Condition of Arch (%s, %s) is not a correct Json Logic Expression.",
                                    graph[source].task.getId(), graph[destination].task.getId()));
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

        final int startingNodeIdx = getNodeIdxFromId(hermesProcess.getStartingNodeId());

        lockingPointers = new ConcurrentHashMap<>();
        lockingPointers.put(startingNodeIdx, new ReentrantLock());
    }

    private int createGraphNodes(HermesProcess hermesProcess)
    {
        int howManyNodeWithVariables = 0;
        final int nodeListSize = hermesProcess.getNodes().size();
        for (int i = 0; i < nodeListSize; ++i)
        {
            var task = hermesProcess.getNodes().get(i);
            graph[i] = new GraphNode(TaskFactory.createNewTaskFrom(task, i));
            Integer numberOfVariables = graph[i].task.getNumberOfVariables();
            if (numberOfVariables != null && numberOfVariables > 0)
                ++howManyNodeWithVariables;
        }
        return howManyNodeWithVariables;
    }

    private void initializeGraphVariables()
    {
        for (GraphNode node : graph) {
            Integer nv;
            if ((nv = node.task.getNumberOfVariables()) != null && nv > 0) {
                graphVariables.put(node.task.getId(), new HashMap<>(nv));
            }
        }
    }

    private void createArches(final HermesProcess hermesProcess)
    {
        try {
            for (final Arch arch : hermesProcess.getArches()) {
                final int sourceIdx = getNodeIdxFromId(arch.src());
                if (graph[sourceIdx].arches == null) {
                    graph[sourceIdx].arches = new ArrayList<>();
                }

                AbstractGraphArch newArch;
                List<Arch.ConditionArch> conditions;
                if ((conditions = arch.conditions()) != null) {
                    newArch = new ConditionalGraphArch(sourceIdx, conditions.size());
                    ConditionalGraphArch cga = (ConditionalGraphArch) newArch;
                    for (var conditionArch : conditions) {
                        cga.addCondition(
                                getNodeIdxFromId(conditionArch.dst()),
                                conditionArch.IF() != null ? JsonLogicParser.parse(conditionArch.IF()) : null
                        );
                    }
                } else {
                    newArch = new GraphArch(sourceIdx, getNodeIdxFromId(arch.dst()));
                }

                graph[sourceIdx].arches.add(newArch);
            }
        } catch (JsonLogicParseException e) {
            log.error(e.getMessage());
            throw new IllegalHermesProcess("");
        }
    }

    private int getNodeIdxFromId(final String id)
    {
        final int size = graph.length;
        for (int i = 0; i < size; ++i) {
            if (graph[i].task.getId().equals(id))
                return i;
        }
        throw new IllegalHermesProcess("Node ID " + id + " not found.");
    }

    private boolean areExternalVariablesValid(final int currentNodeIdx, final Map<String, Object> variables) {
        Integer nv = graph[currentNodeIdx].task.getNumberOfVariables();
        if (nv == null || nv <= 0) {
            return variables == null || variables.isEmpty();
        }
        return variables != null && variables.size() == nv;
    }

    @Override
    public List<ITask> getCurrentTasks()
    {
        if (isEnd < 0)
            return Collections.emptyList();

        final List<ITask> tasks = new ArrayList<>(lockingPointers.size());
        for (int ptr : lockingPointers.keySet()) {
            if (! graph[ptr].isEnding())
                tasks.add(graph[ptr].task);
        }
        return tasks;
    }

    @Override
    public int completeTask(final int currentNodeIdx) {
        return completeTask(currentNodeIdx, null);
    }

    @Override
    public int completeTask(final int currentNodeIdx, final Map<String, Object> variables)
    {
        if (isEnd < 0) return isEnd;

        final Lock lock = lockingPointers.get(currentNodeIdx);
        if (lock == null)
            return LOCK_REJECTED;

        if (! areExternalVariablesValid(currentNodeIdx, variables))
            return INVALID_VARIABLES;

        final GraphNode currentNode = graph[currentNodeIdx];

        Map<String, Object> currentNodeVariables = null;
        if (variables != null && !variables.isEmpty()) {
            currentNodeVariables = graphVariables.get(currentNode.task.getId());
        }

        if (! lock.tryLock())
            return LOCK_REJECTED;
        try {
            if (currentNodeVariables != null) {
                currentNodeVariables.clear();
                currentNodeVariables.putAll(variables);
            }
            return completeMovementWithFork(currentNodeIdx);
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
    private int completeMovementWithFork(final int pointerKey) {
        int next;
        GraphNode node;
        int finalResult = SUCCESS;
        boolean lockReused = false;
        final Queue<GraphNode> nodes = new ArrayDeque<>();

        moveWithFork(pointerKey, nodes);
        if (! nodes.isEmpty())
        {
            while ((node = nodes.poll()) != null)
            {
                final ITask task = node.task;
                next = task.getIdx();

                switch (task.getType())
                {
                    case NORMAL:
                        if (finalResult == SUCCESS && next != pointerKey) {
                            if (lockReused) {
                                lockingPointers.putIfAbsent(next, new ReentrantLock());
                            }
                            else {
                                lockingPointers.putIfAbsent(next, lockingPointers.remove(pointerKey));
                                lockReused = true;
                            }
                        }
                        break;

                    case ENDING:
                        if (finalResult == SUCCESS)
                            finalResult = isEnd = task.isGoodEnding() ? GOOD_ENDING : BAD_ENDING;
                        break;

                    case FORWARD:
                        moveWithFork(next, nodes);
                        break;

                    default:
                        String error = "Invalid Task Type at run time.";
                        log.error(error);
                        throw new IllegalHermesProcess(error);
                }
            }
        }
        lockingPointers.remove(pointerKey);
        return finalResult;
    }

    private void moveWithFork(final int from, final Queue<GraphNode> pointers)
    {
        int destination;
        for (final AbstractGraphArch arch : graph[from].arches) {
            if ((destination = arch.evaluate()) != -1)
                pointers.add(graph[destination]);
        }
    }
}

