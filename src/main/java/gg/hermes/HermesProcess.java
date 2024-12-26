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

        final Map<String, Task> idMap = new HashMap<>(nodes.size());
        final Set<String> endingTasks = new HashSet<>();
        for (var task : nodes) {
            task.validate();
            idMap.put(task.getId(), task);
            if (TaskType.ENDING.equals(task.getType())) {
                endingTasks.add(task.getId());
            }
        }

        if (idMap.size() != nodes.size())
            throw new IllegalHermesProcess("At least 1 node Id is NOT UNIQUE.");

        if (endingTasks.isEmpty())
            throw new IllegalHermesProcess("No ENDING tasks found.");

        Task task;
        if ((task = idMap.get(startingNodeId)) != null) {
            if (! TaskType.NORMAL.equals(task.getType()))
                throw new IllegalHermesProcess("Starting task type is NOT NORMAL.");
        }
        else throw new IllegalHermesProcess("UNRECOGNIZED starting node Id.");


        for (var arch : arches) {
            arch.validate();
            if (endingTasks.contains(arch.source()))
                throw new IllegalHermesProcess("An ENDING task cannot have an ARCH.");

            if (arch.conditions() != null) {
                if (!idMap.containsKey(arch.source()))
                    throw new IllegalHermesProcess("UNRECOGNIZED arch Source.");
                for (var conditionArch : arch.conditions()) {
                    if (!idMap.containsKey(conditionArch.destination()))
                        throw new IllegalHermesProcess("UNRECOGNIZED arch Destination.");
                }
            }
            else {
                if (!idMap.containsKey(arch.source()) || !idMap.containsKey(arch.destination()))
                    throw new IllegalHermesProcess("UNRECOGNIZED arch Source or Destination.");
            }
        }
    }
}
