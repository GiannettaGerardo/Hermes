package gg.hermes.engine;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;

public final class HermesAtomicReferenceArray<T> extends AtomicReferenceArray<T>
{
    public HermesAtomicReferenceArray(int length) {
        super(length);
    }

    public HermesAtomicReferenceArray(T[] array) {
        super(array);
    }

    public T findByPredicate(final Predicate<T> predicate)
    {
        final int size = length();
        for (int i = 0; i < size; ++i)
        {
            T t = getAcquire(i);
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }
}
