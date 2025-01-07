package gg.hermes.nodes;

import gg.hermes.Arch;
import gg.hermes.IllegalHermesProcess;

import java.util.List;

public class HermesNode
{
    private HermesNodeType type;
    private String name;
    private String description;
    private int numberOfVariables;
    private boolean goodEnding;
    private int archesToJoin;
    private List<Arch> to;

    private HermesNode() {}

    public HermesNode(HermesNodeType type, String name, String description, int numberOfVariables,
                      boolean goodEnding, int archesToJoin, List<Arch> to)
    {
        this.type = type;
        this.name = name;
        this.description = description;
        this.numberOfVariables = numberOfVariables;
        this.goodEnding = goodEnding;
        this.archesToJoin = archesToJoin;
        this.to = to;
    }

    public int getId() {
        return -1;
    }

    public HermesNodeType getType() {
        return type;
    }

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    public boolean isGoodEnding() {
        return goodEnding;
    }

    public int getArchesToJoin() {
        return archesToJoin;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Arch> getTo() {
        return to;
    }

    public void setType(HermesNodeType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNumberOfVariables(int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
    }

    public void setGoodEnding(boolean goodEnding) {
        this.goodEnding = goodEnding;
    }

    public void setArchesToJoin(int archesToJoin) {
        this.archesToJoin = archesToJoin;
    }

    public void setTo(List<Arch> to) {
        this.to = to;
    }

    public void validate() {
        switch (type) {
            case NORMAL:
                if (numberOfVariables < 0)
                    throw new IllegalHermesProcess("NORMAL node has LESS THAN ZERO number of variables.");
                break;
            case JOIN:
                if (archesToJoin < 0)
                    throw new IllegalHermesProcess("JOIN node has LESS THAN ZERO number of arches to join.");
                break;
            case FORWARD, ENDING: break;
            default: throw new IllegalHermesProcess("NULL or UNRECOGNIZED node Type.");
        }
    }

    @Override
    public String toString() {
        return "HermesNode{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", numberOfVariables=" + numberOfVariables +
                ", goodEnding=" + goodEnding +
                ", archesToJoin=" + archesToJoin +
                ", to=" + to +
                '}';
    }
}
