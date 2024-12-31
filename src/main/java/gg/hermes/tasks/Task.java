package gg.hermes.tasks;

import gg.hermes.exception.IllegalHermesProcess;

public class Task implements ITask
{
    private String id;
    private TaskType type;
    private String name;
    private String description;
    private Integer numberOfVariables;
    private Boolean goodEnding;

    private Task() {}

    private Task(String id, TaskType type, String name, String description, Integer numberOfVariables, Boolean goodEnding) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.numberOfVariables = numberOfVariables;
        this.goodEnding = goodEnding;
    }

    @Override
    public int getIdx() {
        return -1;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public Integer getNumberOfVariables() {
        return numberOfVariables;
    }

    @Override
    public Boolean isGoodEnding() {
        return goodEnding;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setNumberOfVariables(Integer numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
    }

    public void setGoodEnding(Boolean goodEnding) {
        this.goodEnding = goodEnding;
    }

    public void validate() {
        if (id == null || id.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK task Id.");

        switch (type) {
        case NORMAL:
            if (numberOfVariables != null && numberOfVariables < 0)
                throw new IllegalHermesProcess("NORMAL task has LESS THAN ZERO number of variables.");
            break;
        case ENDING:
            if (goodEnding == null)
                throw new IllegalHermesProcess("NULL good ending.");
            break;
        case FORWARD:
            break;
        default:
            throw new IllegalHermesProcess("NULL or UNRECOGNIZED task Type.");
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", goodEnding=" + goodEnding +
                '}';
    }
}
