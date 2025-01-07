package gg.hermes.nodes;

import gg.hermes.HermesProcess;

import java.util.concurrent.atomic.AtomicInteger;

public class Join extends AbstractHermesNode
{
    private final int archesToJoin;
    private final AtomicInteger waitingPointers;

    public Join(final HermesNode from, final int id, final HermesProcess hermes) {
        super(from, id);

        int counter = 0;
        if (from.getArchesToJoin() <= 0) {
            for (var node : hermes.getProcess()) {
                if (node.getTo() == null) continue;
                for (var arch : node.getTo()) {
                    if (arch.conditions() != null) {
                        for (var cond : arch.conditions()) {
                            if (cond.dst() == id) {
                                ++counter;
                                break;
                            }
                        }
                    } else {
                        if (arch.dst() == id)
                            ++counter;
                    }
                }
            }
            archesToJoin = counter;
        } else {
            archesToJoin = from.getArchesToJoin();
        }

        waitingPointers = new AtomicInteger(0);
    }

    public boolean safeIncrement() {
        int previous;
        while ((previous = waitingPointers.getAcquire()) < archesToJoin
                && !waitingPointers.compareAndSet(previous, ++previous));
        return previous >= archesToJoin;
    }

    public void safeReset() {
        waitingPointers.compareAndSet(archesToJoin, 0);
    }

    @Override
    public int getArchesToJoin() {
        return archesToJoin;
    }

    @Override
    public HermesNodeType getType() {
        return HermesNodeType.JOIN;
    }


    @Override
    public String toString() {
        return "Join{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", archesToJoin=" + archesToJoin +
                ", waitingPointers=" + waitingPointers +
                '}';
    }
}
