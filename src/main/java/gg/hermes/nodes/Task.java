package gg.hermes.nodes;

public class Task extends AbstractHermesNode
{
    private final String idAsString;
    private final Integer numberOfVariables;

    public Task(final HermesNode from, final int id) {
        super(from, id);
        numberOfVariables = from.getNumberOfVariables();
        idAsString = (numberOfVariables > 0) ? Integer.toString(id) : null;
    }

    public String getIdAsString() {
        return idAsString;
    }

    @Override
    public HermesNodeType getType() {
        return HermesNodeType.NORMAL;
    }

    @Override
    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", numberOfVariables=" + numberOfVariables +
                '}';
    }
}
