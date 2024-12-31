package gg.hermes.tasks;

public interface ITask {
    int getIdx();
    String getId();
    String getName();
    String getDescription();
    TaskType getType();
    Integer getNumberOfVariables();
    Boolean isGoodEnding();
}
