package gg.hermes;

import java.util.List;

public record Arch(
        int dst,
        List<ConditionArch> conditions
) {
    public void validate(final int nodeListSize, final int archIndex) {
        if (conditions != null) {
            if (conditions.isEmpty())
                throw new IllegalHermesProcess("EMPTY Condition list.");

            final int size = conditions.size();
            for (int i = 0; i < size; ++i) {
                final ConditionArch ca = conditions.get(i);
                if (ca == null)
                    throw new IllegalHermesProcess("Arch Condition NULL in index " + archIndex);
                if (ca.dst < 0 || ca.dst >= nodeListSize)
                    throw new IllegalHermesProcess("UNRECOGNIZED Arch Destination in Condition index " + i + " and Arch index " + archIndex);

                if ((i == 0 || i < size - 1) && (ca.IF == null || ca.IF.isBlank()))
                        throw new IllegalHermesProcess("NULL or BLANK Condition.");
            }
        }
        else {
            if (dst < 0 || dst >= nodeListSize)
                throw new IllegalHermesProcess("UNRECOGNIZED Arch Destination in index " + archIndex);
        }
    }

    public record ConditionArch(int dst, String IF) {}
}
