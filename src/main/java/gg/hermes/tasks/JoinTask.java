package gg.hermes.tasks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JoinTask extends AbstractTask
{
    private final int archesToJoin;
    private final AtomicInteger waitingPointers;

    public JoinTask(final ITask from, final int idx, final List<Arch> arches) {
        super(from, idx);

        archesToJoin = from.getArchesToJoin() <= 0
                ? (int) arches.stream().filter(arch -> id.equals(arch.dst())).count()
                : from.getArchesToJoin();

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
    public TaskType getType() {
        return TaskType.JOIN;
    }


    @Override
    public String toString() {
        return "JoinTask{" +
                "idx=" + idx +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", archesToJoin=" + archesToJoin +
                ", waitingPointers=" + waitingPointers +
                '}';
    }
}
