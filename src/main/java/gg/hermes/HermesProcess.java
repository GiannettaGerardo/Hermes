package gg.hermes;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        final Set<String> idSet = new HashSet<>(nodes.size());
        for (var task : nodes) {
            task.validate();
            idSet.add(task.getId());
        }

        if (idSet.size() != nodes.size())
            throw new IllegalHermesProcess("At least 1 node Id is NOT UNIQUE.");

        if (! idSet.contains(startingNodeId))
            throw new IllegalHermesProcess("UNRECOGNIZED starting node Id.");

        for (var arch : arches) {
            arch.validate();
            if (!idSet.contains(arch.source()) || !idSet.contains(arch.destination()))
                throw new IllegalHermesProcess("UNRECOGNIZED arch Source or Destination.");
        }
    }
}
