package gg.hermes;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;
import gg.hermes.tasks.TaskType;

import java.util.*;
import java.util.function.Supplier;

public class HermesProcess {
    private List<Task> nodes;
    private List<Arch> arches;
    private String startingNodeId;

    private HermesProcess() {}

    public HermesProcess(List<Task> nodes, List<Arch> arches, String startingNodeId) {
        this.nodes = nodes;
        this.arches = arches;
        this.startingNodeId = startingNodeId;
    }

    public List<Task> getNodes() {
        return nodes;
    }

    public void setNodes(List<Task> nodes) {
        this.nodes = nodes;
    }

    public List<Arch> getArches() {
        return arches;
    }

    public void setArches(List<Arch> arches) {
        this.arches = arches;
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public void setStartingNodeId(String startingNodeId) {
        this.startingNodeId = startingNodeId;
    }

    public void validate() {
        if (nodes == null || nodes.isEmpty())
            throw new IllegalHermesProcess("NULL or EMPTY node list.");

        if (arches == null || arches.isEmpty())
            throw new IllegalHermesProcess("NULL or EMPTY arch list.");

        if (startingNodeId == null || startingNodeId.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK starting node Id.");

        final Map<String, TaskForkData> idMap = new HashMap<>(nodes.size());
        final Set<String> endingTasks = new HashSet<>();
        for (var task : nodes) {
            task.validate();
            idMap.put(task.getId(), new TaskForkData(task));
            if (TaskType.ENDING.equals(task.getType())) {
                endingTasks.add(task.getId());
            }
        }

        if (idMap.size() != nodes.size())
            throw new IllegalHermesProcess("At least 1 node Id is NOT UNIQUE.");

        if (endingTasks.isEmpty())
            throw new IllegalHermesProcess("No ENDING tasks found.");

        TaskForkData firstTaskFork = idMap.get(startingNodeId);
        if (firstTaskFork != null) {
            if (! TaskType.NORMAL.equals(firstTaskFork.task.getType()))
                throw new IllegalHermesProcess("Starting task type is NOT NORMAL.");
        }
        else throw new IllegalHermesProcess("UNRECOGNIZED starting node Id.");


        for (var arch : arches) {
            arch.validate();
            if (endingTasks.contains(arch.src()))
                throw new IllegalHermesProcess("An ENDING task cannot have an ARCH.");

            if (arch.conditions() != null) {
                if (!idMap.containsKey(arch.src()))
                    throw new IllegalHermesProcess("UNRECOGNIZED arch Source.");
                for (var conditionArch : arch.conditions()) {
                    if (!idMap.containsKey(conditionArch.dst()))
                        throw new IllegalHermesProcess("UNRECOGNIZED arch Destination.");
                }
            }
            else {
                if (!idMap.containsKey(arch.src()) || !idMap.containsKey(arch.dst()))
                    throw new IllegalHermesProcess("UNRECOGNIZED arch Source or Destination.");
            }
        }

        forkAndLoopValidation(firstTaskFork, idMap);
    }

    private void forkAndLoopValidation(final TaskForkData first, final Map<String, TaskForkData> idMap)
    {
        first.forkData = new byte[] {0};
        TaskForkData current;
        final List<Arch> sourceArches = new ArrayList<>();

        final Queue<TaskForkData> queue = new ArrayDeque<>();
        queue.add(first);

        while ((current = queue.poll()) != null) {
            resetAndFillBySourceId(sourceArches, current.task.getId());
            if (sourceArches.isEmpty())
                continue;

            final var finalCurrentForkData = current.forkData;
            if (sourceArches.size() > 1) {
                byte newValue = 0;
                for (var arch : sourceArches) {
                    final byte finalNewValue = newValue;
                    if (arch.conditions() != null) {
                        for (var condition : arch.conditions())
                            manageSingleMovement(idMap, queue, condition.dst(), current, () -> newForkData(finalCurrentForkData, finalNewValue));
                    }
                    else manageSingleMovement(idMap, queue, arch.dst(), current, () -> newForkData(finalCurrentForkData, finalNewValue));
                    ++newValue;
                }
            }
            else {
                var arch = sourceArches.get(0);
                if (arch.conditions() != null) {
                    for (var condition : arch.conditions())
                        manageSingleMovement(idMap, queue, condition.dst(), current, () -> finalCurrentForkData);
                }
                else manageSingleMovement(idMap, queue, arch.dst(), current, () -> finalCurrentForkData);
            }
        }
    }

    private void resetAndFillBySourceId(final List<Arch> sourceArches, final String sourceId) {
        sourceArches.clear();
        for (Arch arch : arches) {
            if (arch.src().equals(sourceId)) {
                sourceArches.add(arch);
            }
        }
    }

    private void manageSingleMovement(
            final Map<String, TaskForkData> idMap,
            final Queue<TaskForkData> queue,
            final String destination,
            final TaskForkData current,
            final Supplier<byte[]> newForkData
    ) {
        TaskForkData next;
        byte[] nextForkData;
        byte[] currentForkData;
        next = idMap.get(destination);
        currentForkData = current.forkData;

        if ((nextForkData = next.forkData) == null) {
            next.forkData = newForkData.get();
            queue.add(next);
        }
        else if (nextForkData.length < currentForkData.length) {
            final int size = nextForkData.length;
            for (int i = 0; i < size; ++i) {
                if (nextForkData[i] != currentForkData[i])
                    throw new IllegalHermesProcess(String.format("Hermes do not allow arch ('%s', '%s') in this graph.", current.task.getId(), next.task.getId()));
            }
        }
        else if (nextForkData.length > currentForkData.length || nextForkData != currentForkData)
            throw new IllegalHermesProcess(String.format("Hermes do not allow arch ('%s', '%s') in this graph.", current.task.getId(), next.task.getId()));
    }

    private static byte[] newForkData(final byte[] currentForkData, final byte newValue) {
        final int size = currentForkData.length;
        final byte[] newForkData = new byte[size + 1];
        System.arraycopy(currentForkData, 0, newForkData, 0, size);
        newForkData[size] = newValue;
        return newForkData;
    }

    private static class TaskForkData {
        final Task task;
        byte[] forkData;

        public TaskForkData(Task task) {
            this.task = task;
            forkData = null;
        }
    }
}
