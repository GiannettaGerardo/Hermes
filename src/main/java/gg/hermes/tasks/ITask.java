package gg.hermes.tasks;

public interface ITask {
    int getIdx();
    String getId();
    String getName();
    String getDescription();

    default TaskType getType() {
        return null;
    }

    default int getNumberOfVariables() {
        return 0;
    }

    default boolean isGoodEnding() {
        return false;
    }

    default int getArchesToJoin() {
        return 0;
    }
}
