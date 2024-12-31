package gg.hermes.tasks;

import gg.hermes.exception.IllegalHermesProcess;

import java.util.List;

public record Arch(
        String src,
        String dst,
        List<ConditionArch> conditions
) {
    public void validate() {
        if (src == null || src.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK arch Source.");

        if (conditions != null) {
            if (dst != null)
                throw new IllegalHermesProcess("Destination must be NULL when the Arch has a Condition.");

            if (conditions.isEmpty())
                throw new IllegalHermesProcess("EMPTY Condition list.");

            final int size = conditions.size();
            for (int i = 0; i < size; ++i) {
                final ConditionArch ca = conditions.get(i);
                if (ca.dst == null || ca.dst.isBlank())
                    throw new IllegalHermesProcess("NULL or BLANK arch Destination in Condition Arch.");

                if ((i == 0 || i < size - 1) && (ca.IF == null || ca.IF.isBlank()))
                        throw new IllegalHermesProcess("NULL or BLANK Condition.");
            }
        }
        else {
            if (dst == null || dst.isBlank())
                throw new IllegalHermesProcess("NULL or BLANK arch Destination.");
        }
    }

    public record ConditionArch(String dst, String IF) {}
}
