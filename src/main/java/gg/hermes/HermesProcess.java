package gg.hermes;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;
import gg.hermes.tasks.TaskType;

import java.util.*;

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

    public List<Arch> getArches() {
        return arches;
    }

    public String getStartingNodeId() {
        return startingNodeId;
    }

    public void validate() {
        if (nodes == null || nodes.isEmpty())
            throw new IllegalHermesProcess("NULL or EMPTY node list.");

        if (arches == null || arches.isEmpty())
            throw new IllegalHermesProcess("NULL or EMPTY arch list.");

        if (startingNodeId == null || startingNodeId.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK starting node Id.");

        final Map<String, Task> idMap = new HashMap<>(nodes.size());
        final Set<String> endingTasks = new HashSet<>();
        checkTasks(idMap, endingTasks);

        checkFirstTask(idMap);

        checkArches(idMap, endingTasks);
    }

    private void checkJoinNode(final Task task) {
        int joinActualCount = 0;
        for (var arch : arches) {
            if (task.getId().equals(arch.dst())) {
                if (arch.src().equals(arch.dst()))
                    throw new IllegalHermesProcess("JOIN Task cannot have a Self Reference Arch.");
                ++joinActualCount;
            }
        }
        if (joinActualCount < task.getArchesToJoin())
            throw new IllegalHermesProcess("Arches to Join attribute is greater than the actual count of arches that enter in the Task node.");
    }

    private void checkTask(final Task task, final Map<String, Task> idMap, final Set<String> endingTasks) {
        task.validate();
        idMap.put(task.getId(), task);
        if (task.getType() == TaskType.ENDING)
            endingTasks.add(task.getId());
        else if (task.getType() == TaskType.JOIN) {
            checkJoinNode(task);
        }
    }

    private void checkTasks(final Map<String, Task> idMap, final Set<String> endingTasks) {
        for (var task : nodes)
            checkTask(task, idMap, endingTasks);

        if (idMap.size() != nodes.size())
            throw new IllegalHermesProcess("At least 1 node Id is NOT UNIQUE.");

        if (endingTasks.isEmpty())
            throw new IllegalHermesProcess("No ENDING tasks found.");
    }

    private void checkFirstTask(final Map<String, Task> idMap) {
        final Task firstTask = idMap.get(startingNodeId);
        if (firstTask != null) {
            if (! TaskType.NORMAL.equals(firstTask.getType()))
                throw new IllegalHermesProcess("Starting task type is NOT NORMAL.");
        }
        else throw new IllegalHermesProcess("UNRECOGNIZED starting node Id.");
    }

    private void checkArches(final Map<String, Task> idMap, final Set<String> endingTasks) {
        for (var arch : arches) {
            arch.validate();
            if (endingTasks.contains(arch.src()))
                throw new IllegalHermesProcess("An ENDING task cannot have an ARCH.");

            if (arch.conditions() != null)
                checkConditionArch(arch, idMap);
            else {
                if (!idMap.containsKey(arch.src()) || !idMap.containsKey(arch.dst()))
                    throw new IllegalHermesProcess("UNRECOGNIZED arch Source or Destination.");
            }
        }
    }

    private void checkConditionArch(final Arch arch, final Map<String, Task> idMap) {
        if (!idMap.containsKey(arch.src()))
            throw new IllegalHermesProcess("UNRECOGNIZED arch Source.");
        for (var conditionArch : arch.conditions()) {
            if (!idMap.containsKey(conditionArch.dst()))
                throw new IllegalHermesProcess("UNRECOGNIZED arch Destination.");
        }
    }
}
