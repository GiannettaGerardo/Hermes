package gg.hermes.tasks;

import gg.hermes.exception.IllegalHermesProcess;

public final class TaskFactory
{
    private TaskFactory() {}

    public static ITask createNewTaskFrom(ITask task, int idx) {
        switch (task.getType()) {
        case NORMAL:
            return new NormalTask(task, idx);
        case ENDING:
            return new EndingTask(task, idx);
        case FORWARD:
            return new ForwardTask(task, idx);
        default:
            throw new IllegalHermesProcess("Illegal Task Type not found.");
        }
    }
}
