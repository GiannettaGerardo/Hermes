package gg.hermes;

import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;

import java.util.List;

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
}
