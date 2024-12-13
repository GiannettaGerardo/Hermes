package gg.hermes.engine;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.model.HermesProcess;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class HermesConcurrentGraph implements HermesGraph
{
    private final Logger log;
    private final GraphNode[] graph;
    private final HermesAtomicReferenceArray<GraphPtr> ptrList;
    private final JsonLogicEvaluator jsonLogicEvaluator;
    private final int startingNodeIdx;

    public HermesConcurrentGraph(HermesProcess hermesProcess, JsonLogicConfiguration jsonLogicConf, Logger log)
    {
        this.log = log;

        // TODO bisogna validare tutto prima

        final int nodeListSize = hermesProcess.nodes().size();
        graph = new GraphNode[nodeListSize];

        for (int i = 0; i < nodeListSize; ++i)
        {
            var task = hermesProcess.nodes().get(i);
            graph[i] = new GraphNode(TaskFactory.createNewTaskFrom(task, i));
        }

        jsonLogicEvaluator = jsonLogicConf.getEvaluator();
        try {
            createArches(hermesProcess);
        }
        catch (JsonLogicParseException e) {
            log.error(e.getMessage());
            throw new IllegalHermesProcess("");
        }

        startingNodeIdx = hermesProcess.startingNodeIdx();

        ptrList = new HermesAtomicReferenceArray<>(1);
        ptrList.set(0, new GraphPtr(startingNodeIdx));
    }

    private int getNodeIdxFromId(final String id)
    {
        final int size = graph.length;
        for (int i = 0; i < size; ++i) {
            if (graph[i].task.getId().equals(id))
                return i;
        }
        throw new IllegalHermesProcess("");
    }

    private void createArches(final HermesProcess hermesProcess) throws JsonLogicParseException
    {
        for (Arch arch : hermesProcess.arches())
        {
            final int sourceIdx = getNodeIdxFromId(arch.source());
            final int destinationIdx = getNodeIdxFromId(arch.destination());
            if (graph[sourceIdx].arches == null) {
                graph[sourceIdx].arches = new ArrayList<>();
            }

            JsonLogicNode parsedCondition = null;
            if (arch.condition() != null) {
                parsedCondition = JsonLogicParser.parse(arch.condition().getRule()); // TODO nella validazione, validare getRule != (null e blank)
            }

            graph[sourceIdx].arches.add(new GraphArch(sourceIdx, destinationIdx, parsedCondition));
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

    @Override
    public int completeTask(final int currentNodeIdx)
    {
        int next;
        GraphNode node;
        final GraphPtr ptr = ptrList.findByPredicate(x -> x.idx == currentNodeIdx);
        if (ptr == null)
            return RESULT_LOCK_REJECTED;
        int previous = ptr.idx;

        if (graph[previous].task.isGoodEnding() != null)
            return graph[previous].task.isGoodEnding() ? RESULT_GOOD_ENDING : RESULT_BAD_ENDING;

        if (! ptr.lock.tryLock())
            return RESULT_LOCK_REJECTED;
        try {
            while ((node = move(previous)) != null)
            {
                next = node.task.getIdx();
                if (! node.task.isSpecial()) {
                    ptr.idx = next;
                    return RESULT_OK;
                }
                previous = next;

                final ITask task = node.task;
                switch (task.getType()) {
                case ENDING:
                    ptr.idx = previous;
                    return task.isGoodEnding() ? RESULT_GOOD_ENDING : RESULT_BAD_ENDING;
                case FORWARD:
                    break;
                default:
                    log.error("Invalid Task Type.");
                    return RESULT_INVALID_RESOLVE;
                }
            }
            return RESULT_NO_MOVE;
        }
        finally {
            ptr.lock.unlock();
        }
    }

    private GraphNode move(final int from)
    {
        for (final GraphArch arch : graph[from].arches) {
            if (arch.isCondition()) {
                if (arch.evaluate(null)) { // TODO passare data
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

        public boolean isCondition() {
            return condition != null;
        }

        public boolean evaluate(final Map<String, Object> data) {
            try {
                return (boolean) jsonLogicEvaluator.evaluate(condition, data);
            }
            catch (JsonLogicException | ClassCastException e) {
                log.error(e.getMessage());
                throw new IllegalHermesProcess("Condition of Arch ("
                        + graph[source].task.getId()
                        + ", "
                        + graph[destination].task.getId()
                        + ") is not a correct Json Logic Expression.");
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

