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
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class HermesConcurrentGraph implements HermesGraph
{
    private final Logger log;
    private final GraphNode[] graph;
    private final Map<String, Map<String, Object>> nodeVariables;
    private final AtomicReferenceArray<GraphPtr> ptrList;
    private final JsonLogicEvaluator jsonLogicEvaluator;
    private final int startingNodeIdx;

    public HermesConcurrentGraph(HermesProcess hermesProcess, JsonLogicConfiguration jsonLogicConf, Logger log)
    {
        this.log = log;

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
            if (numberOfVariables != null && numberOfVariables > 0) {
                graph[i].variables = new HashMap<>(numberOfVariables);
                ++howManyNodeWithVariables;
            }
        }

        nodeVariables = new HashMap<>(howManyNodeWithVariables);
        for (GraphNode node : graph) {
            if (node.variables != null) {
                nodeVariables.put(node.task.getId(), node.variables);
            }
        }

        jsonLogicEvaluator = jsonLogicConf.getEvaluator();
        createArches(hermesProcess);

        startingNodeIdx = getNodeIdxFromId(hermesProcess.getStartingNodeId());

        ptrList = new AtomicReferenceArray<>(1);
        ptrList.set(0, new GraphPtr(startingNodeIdx));
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

    private GraphPtr findPtrById(final int toFind) {
        final int size = ptrList.length();
        for (int i = 0; i < size; ++i) {
            GraphPtr ptr = ptrList.getAcquire(i);
            if (ptr.idx == toFind) {
                return ptr;
            }
        }
        return null;
    }

    private void variablesRollback(final GraphNode currentNode) {
        if (currentNode.variables != null && !currentNode.variables.isEmpty()) {
            final Map<String, Object> emptyMap = new HashMap<>(currentNode.task.getNumberOfVariables());
            currentNode.variables = emptyMap;
            nodeVariables.replace(currentNode.task.getId(), emptyMap);
        }
    }

    @Override
    public List<ITask> getCurrentTasks()
    {
        GraphNode node;
        final int size = ptrList.length();
        // TODO attualmente ritorna riferimenti diretti ai Task. Valutare di ritornare cloni DTO dei Task
        final List<ITask> tasks = new ArrayList<>(size);
        for (int i = 0; i < size; ++i)
        {
            node = graph[ptrList.getAcquire(i).idx];
            if (node == null || TaskType.ENDING.equals(node.task.getType()))
                continue;

            tasks.add(node.task);
        }
        return tasks;
    }

    private boolean areExternalVariablesValid(final int currentNodeIdx, final Map<String, Object> variables) {
        Integer nv = graph[currentNodeIdx].task.getNumberOfVariables();
        if (nv == null || nv <= 0) {
            return variables == null || variables.isEmpty();
        }
        return variables != null && variables.size() == nv;
    }

    @Override
    public int completeTask(final int currentNodeIdx) {
        return completeTask(currentNodeIdx, null);
    }

    @Override
    public int completeTask(final int currentNodeIdx, final Map<String, Object> variables)
    {
        final GraphPtr ptr = findPtrById(currentNodeIdx);
        if (ptr == null)
            return LOCK_REJECTED;

        if (! areExternalVariablesValid(currentNodeIdx, variables))
            return INVALID_VARIABLES;

        final GraphNode currentNode = graph[currentNodeIdx];
        if (currentNode.isEnding())
            return currentNode.task.isGoodEnding() ? GOOD_ENDING : BAD_ENDING;

        if (! ptr.lock.tryLock())
            return LOCK_REJECTED;
        try {
            if (variables != null && !variables.isEmpty()) {
                // in case of rollback, "currentNode.variables" will be an empty map again
                currentNode.variables.putAll(variables);
            }
            final int result = completeMovement(ptr);
            if (result > 1) {
                variablesRollback(currentNode);
            }
            return result;
        }
        catch (final Exception exception) {
            variablesRollback(currentNode);
            throw exception;
        }
        finally {
            ptr.lock.unlock();
        }
    }

    private int completeMovement(final GraphPtr ptr) {
        GraphNode node;
        int next;
        int previous = ptr.idx;
        while ((node = move(previous)) != null)
        {
            next = node.task.getIdx();
            if (! node.task.isSpecial()) {
                ptr.idx = next;
                return SUCCESS;
            }
            previous = next;

            final ITask task = node.task;
            switch (task.getType()) {
            case ENDING:
                ptr.idx = previous;
                return task.isGoodEnding() ? GOOD_ENDING : BAD_ENDING;
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
        Map<String, Object> variables;

        public GraphNode(ITask task) {
            this.task = task;
            arches = null;
            variables = null;
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
                return (boolean) jsonLogicEvaluator.evaluate(condition, nodeVariables);
            }
            catch (JsonLogicException | ClassCastException e) {
                log.error(e.getMessage());
                throw new IllegalHermesProcess(
                        String.format("Condition of Arch (%s, %s) is not a correct Json Logic Expression.",
                                graph[source].task.getId(), graph[destination].task.getId()));
            }
        }
    }

    private static class GraphPtr {
        volatile int idx;
        final Lock lock;

        public GraphPtr(int idx) {
            this.idx = idx;
            lock = new ReentrantLock(true);
        }
    }
}

