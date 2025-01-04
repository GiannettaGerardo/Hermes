package gg.hermes.tasks;

abstract class AbstractTask implements ITask
{
    protected final int id;
    protected final String name;
    protected final String description;

    protected AbstractTask(final ITask from, final int id) {
        this.id = id;
        name = from.getName();
        description = from.getDescription();
    }

    @Override
    public int getId() {
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
