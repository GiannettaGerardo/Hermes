package gg.hermes.tasks;

public class ForwardTask extends AbstractTask
{
    public ForwardTask(final ITask from, final int id) {
        super(from, id);
    }

    @Override
    public TaskType getType() {
        return TaskType.FORWARD;
    }

    @Override
    public String toString() {
        return "ForwardTask{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
