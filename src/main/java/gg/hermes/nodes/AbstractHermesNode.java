package gg.hermes.nodes;

abstract class AbstractHermesNode implements HermesTask
{
    protected final int id;
    protected final String name;
    protected final String description;

    protected AbstractHermesNode(final HermesNode from, final int id) {
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
