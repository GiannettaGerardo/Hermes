package gg.hermes.tasks;

public class NormalTask extends AbstractTask
{
    private final Integer numberOfVariables;

    public NormalTask(final ITask from, final int idx) {
        super(from, idx);
        numberOfVariables = from.getNumberOfVariables();
    }

    @Override
    public TaskType getType() {
        return TaskType.NORMAL;
    }

    @Override
    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    @Override
    public String toString() {
        return "NormalTask{" +
                "idx=" + idx +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", numberOfVariables=" + numberOfVariables +
                '}';
    }
}
