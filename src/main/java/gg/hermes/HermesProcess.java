package gg.hermes;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;
import gg.hermes.tasks.TaskType;

import java.util.*;
import java.util.stream.Collectors;

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
        for (var task : nodes) {
            task.validate();
            idMap.put(task.getId(), task);
        }

        if (idMap.size() != nodes.size())
            throw new IllegalHermesProcess("At least 1 node Id is NOT UNIQUE.");

        Task task;
        if ((task = idMap.get(startingNodeId)) != null) {
            if (! TaskType.NORMAL.equals(task.getType()))
                throw new IllegalHermesProcess("Starting task type is NOT NORMAL.");
        }
        else throw new IllegalHermesProcess("UNRECOGNIZED starting node Id.");


        for (var arch : arches) {
            arch.validate();
            if (!idMap.containsKey(arch.source()) || !idMap.containsKey(arch.destination()))
                throw new IllegalHermesProcess("UNRECOGNIZED arch Source or Destination.");
        }
    }
}
