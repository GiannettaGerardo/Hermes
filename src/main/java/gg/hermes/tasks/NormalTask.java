package gg.hermes.tasks;

public class NormalTask extends AbstractTask
{
    private final String idAsString;
    private final Integer numberOfVariables;

    public NormalTask(final ITask from, final int id) {
        super(from, id);
        numberOfVariables = from.getNumberOfVariables();
        idAsString = (numberOfVariables > 0) ? Integer.toString(id) : null;
    }

    public String getIdAsString() {
        return idAsString;
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
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", numberOfVariables=" + numberOfVariables +
                '}';
    }
}
