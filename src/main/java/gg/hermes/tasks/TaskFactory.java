package gg.hermes.tasks;

import gg.hermes.HermesProcess;
import gg.hermes.exception.IllegalHermesProcess;

public final class TaskFactory
{
    private TaskFactory() {}

    public static ITask createNewTaskFrom(ITask task, int id, HermesProcess process) {
        switch (task.getType()) {
            case NORMAL: return new NormalTask(task, id);
            case ENDING: return new EndingTask(task, id);
            case FORWARD: return new ForwardTask(task, id);
            case JOIN: return new JoinTask(task, id, process.getArches());
            default: throw new IllegalHermesProcess("Illegal Task Type not found.");
        }
    }
}
