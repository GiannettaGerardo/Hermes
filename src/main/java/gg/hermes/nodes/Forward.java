package gg.hermes.nodes;

public class Forward extends AbstractHermesNode
{
    public Forward(final HermesNode from, final int id) {
        super(from, id);
    }

    @Override
    public HermesNodeType getType() {
        return HermesNodeType.FORWARD;
    }

    @Override
    public String toString() {
        return "Forward{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
