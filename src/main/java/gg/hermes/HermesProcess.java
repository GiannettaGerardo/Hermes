package gg.hermes;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;
import gg.hermes.tasks.TaskType;

import java.util.*;

public class HermesProcess
{
    private List<Task> nodes;
    private List<Arch> arches;
    private int startingNodeId;

    private HermesProcess() {}

    public HermesProcess(List<Task> nodes, List<Arch> arches, int startingNodeId) {
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

    public int getStartingNodeId() {
        return startingNodeId;
    }

    public void validate() {
        if (nodes == null || nodes.isEmpty())
            throw new IllegalHermesProcess("NULL or EMPTY node list.");

        if (arches == null || arches.isEmpty())
            throw new IllegalHermesProcess("NULL or EMPTY arch list.");

        if (startingNodeId < 0 || startingNodeId >= nodes.size())
            throw new IllegalHermesProcess("Starting node Id is not between [0, nodes.size-1].");

        final Set<Integer> endingTasks = new HashSet<>();
        checkTasks(endingTasks);

        checkFirstTask();

        checkArches(endingTasks);
    }

    private void checkJoinNode(final Task task) {
        int joinActualCount = 0;
        for (var arch : arches) {
            if (task.getId() == arch.dst()) {
                if (arch.src() == arch.dst())
                    throw new IllegalHermesProcess("JOIN Task cannot have a Self Reference Arch.");
                ++joinActualCount;
            }
        }
        if (joinActualCount < task.getArchesToJoin())
            throw new IllegalHermesProcess("Arches to Join attribute is greater than the actual count of arches that enter in the Task node.");
    }

    private void checkTasks(final Set<Integer> endingTasks) {
        final int size = nodes.size();
        for (int i = 0; i < size; ++i) {
            var task = nodes.get(i);
            if (task == null)
                throw new IllegalHermesProcess("Task " + i + " is NULL.");
            task.validate();
            if (task.getType() == TaskType.ENDING)
                endingTasks.add(task.getId());
            else if (task.getType() == TaskType.JOIN)
                checkJoinNode(task);
        }

        if (endingTasks.isEmpty())
            throw new IllegalHermesProcess("No ENDING tasks found.");
    }

    private void checkFirstTask() {
        final Task firstTask = nodes.get(startingNodeId);
        if (TaskType.NORMAL != firstTask.getType())
            throw new IllegalHermesProcess("Starting Task Type is NOT NORMAL.");
    }

    private void checkArches(final Set<Integer> endingTasks) {
        final int archesSize = arches.size();
        final int nodesSize = nodes.size();
        for (int i = 0; i < archesSize; ++i) {
            var arch = arches.get(i);
            if (arch == null)
                throw new IllegalHermesProcess("Arch in index " + i + " is NULL.");
            arch.validate(nodesSize, i);
            if (endingTasks.contains(arch.src()))
                throw new IllegalHermesProcess("An ENDING task cannot have an ARCH.");
        }
    }
}
