package gg.hermes.tasks;

public class Task implements ITask
{
    private transient int idx;
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
        return idx;
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
    public boolean isSpecial() {
        return type.equals(TaskType.FORWARD) || type.equals(TaskType.ENDING);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
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
