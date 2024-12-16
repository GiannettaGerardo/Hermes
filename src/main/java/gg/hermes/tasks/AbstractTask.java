package gg.hermes.tasks;

abstract class AbstractTask implements ITask
{
    protected int idx;
    protected final String id;
    protected final String name;
    protected final String description;

    protected AbstractTask(final ITask from, final int idx) {
        this.idx = idx;
        id = from.getId();
        name = from.getName();
        description = from.getDescription();
    }

    @Override
    public int getIdx() {
        return idx;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
