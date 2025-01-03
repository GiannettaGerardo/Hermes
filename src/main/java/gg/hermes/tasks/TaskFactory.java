package gg.hermes.tasks;

import gg.hermes.HermesProcess;
import gg.hermes.exception.IllegalHermesProcess;

public final class TaskFactory
{
    private TaskFactory() {}

    public static ITask createNewTaskFrom(ITask task, int idx, HermesProcess process) {
        switch (task.getType()) {
            case NORMAL: return new NormalTask(task, idx);
            case ENDING: return new EndingTask(task, idx);
            case FORWARD: return new ForwardTask(task, idx);
            case JOIN: return new JoinTask(task, idx, process.getArches());
            default: throw new IllegalHermesProcess("Illegal Task Type not found.");
        }
    }
}
