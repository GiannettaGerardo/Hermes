package gg.hermes.tasks;

public class EndingTask extends AbstractTask
{
    private final Boolean goodEnding;

    public EndingTask(final ITask from, final int idx) {
        super(from, idx);
        goodEnding = from.isGoodEnding();
    }

    @Override
    public TaskType getType() {
        return TaskType.ENDING;
    }

    @Override
    public Integer getNumberOfVariables() {
        return null;
    }

    @Override
    public Boolean isGoodEnding() {
        return goodEnding;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String toString() {
        return "EndingTask{" +
                "idx=" + idx +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", goodEnding=" + goodEnding +
                '}';
    }
}
