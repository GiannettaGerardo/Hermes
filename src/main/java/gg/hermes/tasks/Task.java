package gg.hermes.tasks;

import gg.hermes.exception.IllegalHermesProcess;

public class Task implements ITask
{
    private TaskType type;
    private String name;
    private String description;
    private int numberOfVariables;
    private boolean goodEnding;
    private int archesToJoin;

    private Task() {}

    public Task(TaskType type, String name, String description,
                int numberOfVariables, boolean goodEnding, int archesToJoin) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.numberOfVariables = numberOfVariables;
        this.goodEnding = goodEnding;
        this.archesToJoin = archesToJoin;
    }

    @Override
    public int getId() {
        return -1;
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    @Override
    public boolean isGoodEnding() {
        return goodEnding;
    }

    @Override
    public int getArchesToJoin() {
        return archesToJoin;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setType(TaskType type) {
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

    public void validate() {
        switch (type) {
            case NORMAL:
                if (numberOfVariables < 0)
                    throw new IllegalHermesProcess("NORMAL task has LESS THAN ZERO number of variables.");
                break;
            case JOIN:
                if (archesToJoin < 0)
                    throw new IllegalHermesProcess("JOIN task has LESS THAN ZERO number of arches to join.");
                break;
            case FORWARD, ENDING: break;
            default: throw new IllegalHermesProcess("NULL or UNRECOGNIZED task Type.");
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", numberOfVariables=" + numberOfVariables +
                ", goodEnding=" + goodEnding +
                ", archesToJoin=" + archesToJoin +
                '}';
    }
}
