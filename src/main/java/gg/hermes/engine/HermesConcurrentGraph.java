package gg.hermes.engine;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.HermesProcess;
import gg.hermes.tasks.Arch;
import gg.hermes.tasks.ITask;
import gg.hermes.tasks.TaskFactory;
import gg.hermes.tasks.TaskType;
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
    private final Logger log;
    private final GraphNode[] graph;
    private final Map<String, Map<String, Object>> graphVariables;
    private final Map<Integer, Lock> lockingPointers;
    private final JsonLogicEvaluator jsonLogicEvaluator;
    private final int startingNodeIdx;
    private volatile boolean isEnd;
    private volatile boolean isEndWithGoodEnding;

    public HermesConcurrentGraph(HermesProcess hermesProcess, JsonLogicConfiguration jsonLogicConf, Logger log)
    {
        this.log = log;
        isEnd = isEndWithGoodEnding = false;

        // complete validation of the process
        hermesProcess.validate();

        final int nodeListSize = hermesProcess.getNodes().size();
        graph = new GraphNode[nodeListSize];

        int howManyNodeWithVariables = 0;
        for (int i = 0; i < nodeListSize; ++i)
        {
            var task = hermesProcess.getNodes().get(i);
            graph[i] = new GraphNode(TaskFactory.createNewTaskFrom(task, i));
            Integer numberOfVariables = graph[i].task.getNumberOfVariables();
            if (numberOfVariables != null && numberOfVariables > 0)
                ++howManyNodeWithVariables;
        }

        graphVariables = new HashMap<>(howManyNodeWithVariables);
        for (GraphNode node : graph) {
            Integer nv;
            if ((nv = node.task.getNumberOfVariables()) != null && nv > 0) {
                graphVariables.put(node.task.getId(), new HashMap<>(nv));
            }
        }

        jsonLogicEvaluator = jsonLogicConf.getEvaluator();
        createArches(hermesProcess);

        startingNodeIdx = getNodeIdxFromId(hermesProcess.getStartingNodeId());

        lockingPointers = new ConcurrentHashMap<>();
        lockingPointers.put(startingNodeIdx, new ReentrantLock());
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

    private void createArches(final HermesProcess hermesProcess)
    {
        try {
            for (final Arch arch : hermesProcess.getArches()) {
                final int sourceIdx = getNodeIdxFromId(arch.source());
                final int destinationIdx = getNodeIdxFromId(arch.destination());
                if (graph[sourceIdx].arches == null) {
                    graph[sourceIdx].arches = new ArrayList<>();
                }

                JsonLogicNode parsedCondition = null;
                if (arch.condition() != null) {
                    parsedCondition = JsonLogicParser.parse(arch.condition());
                }

                graph[sourceIdx].arches.add(new GraphArch(sourceIdx, destinationIdx, parsedCondition));
            }
        } catch (JsonLogicParseException e) {
            log.error(e.getMessage());
            throw new IllegalHermesProcess("");
        }
    }

    private void completeTaskRollback(final GraphNode currentNode) {
        var variables = graphVariables.get(currentNode.task.getId());
        if (variables != null && !variables.isEmpty()) {
            final Map<String, Object> emptyMap = new HashMap<>(currentNode.task.getNumberOfVariables());
            graphVariables.replace(currentNode.task.getId(), emptyMap);
        }
    }

    private boolean areExternalVariablesValid(final int currentNodeIdx, final Map<String, Object> variables) {
        Integer nv = graph[currentNodeIdx].task.getNumberOfVariables();
        if (nv == null || nv <= 0) {
            return variables == null || variables.isEmpty();
        }
        return variables != null && variables.size() == nv;
    }

    private int resolveGoodOrBadEnding(boolean isGoodEnding) {
        return isGoodEnding ? GOOD_ENDING : BAD_ENDING;
    }

    @Override
    public List<ITask> getCurrentTasks()
    {
        if (isEnd)
            return Collections.emptyList();

        // TODO attualmente ritorna riferimenti diretti ai Task. Valutare di ritornare cloni DTO dei Task
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
        if (isEnd) return resolveGoodOrBadEnding(isEndWithGoodEnding);

        final Lock lock = lockingPointers.get(currentNodeIdx);
        if (lock == null)
            return LOCK_REJECTED;

        if (! areExternalVariablesValid(currentNodeIdx, variables))
            return INVALID_VARIABLES;

        final GraphNode currentNode = graph[currentNodeIdx];
        if (currentNode.isEnding())
            return resolveGoodOrBadEnding(currentNode.task.isGoodEnding());

        Map<String, Object> currentNodeVariables = null;
        if (variables != null && !variables.isEmpty()) {
            currentNodeVariables = graphVariables.get(currentNode.task.getId());
        }

        if (! lock.tryLock())
            return LOCK_REJECTED;
        try {
            if (currentNodeVariables != null) {
                // in case of rollback, variables of this node will be empty again
                currentNodeVariables.putAll(variables);
            }
            final int result = completeMovement(currentNodeIdx);
            if (result > 1) {
                completeTaskRollback(currentNode);
            }
            return result;
        }
        catch (final Exception exception) {
            completeTaskRollback(currentNode);
            throw exception;
        }
        finally {
            lock.unlock();
        }
    }

    private int completeMovement(final int pointerKey) {
        GraphNode node;
        int previous = pointerKey;
        int next;
        while ((node = move(previous)) != null)
        {
            next = node.task.getIdx();
            if (! node.task.isSpecial()) {
                lockingPointers.put(next, lockingPointers.remove(pointerKey));
                return SUCCESS;
            }
            previous = next;

            final ITask task = node.task;
            switch (task.getType()) {
            case ENDING:
                lockingPointers.remove(pointerKey);
                isEnd = true;
                isEndWithGoodEnding = task.isGoodEnding();
                return resolveGoodOrBadEnding(task.isGoodEnding());
            case FORWARD:
                break;
            default:
                log.error("Invalid Task Type.");
                return INVALID_RESOLVE;
            }
        }
        return NO_MOVE;
    }

    private GraphNode move(final int from)
    {
        for (final GraphArch arch : graph[from].arches) {
            if (arch.isConditional()) {
                if (arch.evaluate()) {
                    return graph[arch.destination];
                }
            }
            else {
                // TODO fork obbligatorio in futuro
                return graph[arch.destination];
            }
        }
        return null;
    }

    private static class GraphNode {
        final ITask task;
        List<GraphArch> arches;

        public GraphNode(ITask task) {
            this.task = task;
            arches = null;
        }

        public boolean isEnding() {
            return TaskType.ENDING.equals(task.getType());
        }
    }

    private class GraphArch
    {
        final int source;
        final int destination;
        final JsonLogicNode condition;

        public GraphArch(int source, int destination, JsonLogicNode condition) {
            this.source = source;
            this.destination = destination;
            this.condition = condition;
        }

        public boolean isConditional() {
            return condition != null;
        }

        public boolean evaluate() {
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

