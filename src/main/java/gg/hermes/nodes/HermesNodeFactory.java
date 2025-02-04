package gg.hermes.nodes;

import gg.hermes.IllegalHermesProcess;

public final class HermesNodeFactory
{
    private HermesNodeFactory() {}

    public static HermesTask createNewTaskFrom(HermesNode node, int id) {
        switch (node.getType()) {
            case TASK: return new Task(node, id);
            case ENDING: return new Ending(node, id);
            case FORWARD: return new Forward(node, id);
            case JOIN: return new Join(node, id);
            default: throw new IllegalHermesProcess("Node Type not found.");
        }
    }
}
