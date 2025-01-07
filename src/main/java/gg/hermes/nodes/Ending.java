package gg.hermes.nodes;

public class Ending extends AbstractHermesNode
{
    private final Boolean goodEnding;

    public Ending(final HermesNode from, final int id) {
        super(from, id);
        goodEnding = from.isGoodEnding();
    }

    @Override
    public HermesNodeType getType() {
        return HermesNodeType.ENDING;
    }

    @Override
    public boolean isGoodEnding() {
        return goodEnding;
    }

    @Override
    public String toString() {
        return "Ending{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", goodEnding=" + goodEnding +
                '}';
    }
}
