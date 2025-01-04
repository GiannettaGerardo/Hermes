package gg.hermes.tasks;

public interface ITask {
    int getId();
    String getName();
    String getDescription();

    default String getIdAsString() {
        return null;
    }

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
