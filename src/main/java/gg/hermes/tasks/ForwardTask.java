package gg.hermes.tasks;

public class ForwardTask extends AbstractTask
{
    public ForwardTask(final ITask from, final int idx) {
        super(from, idx);
    }

    @Override
    public TaskType getType() {
        return TaskType.FORWARD;
    }

    @Override
    public String toString() {
        return "ForwardTask{" +
                "idx=" + idx +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
