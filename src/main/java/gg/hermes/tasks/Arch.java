package gg.hermes.tasks;

import gg.hermes.exception.IllegalHermesProcess;

import java.util.List;

public record Arch(
        String source,
        String destination,
        List<ConditionArch> conditions
) {
    public void validate() {
        if (source == null || source.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK arch Source.");

        if (conditions != null) {
            if (destination != null)
                throw new IllegalHermesProcess("Destination must be NULL when the Arch has a Condition.");

            if (conditions.isEmpty())
                throw new IllegalHermesProcess("EMPTY Condition list.");

            final int size = conditions.size();
            for (int i = 0; i < size; ++i) {
                final ConditionArch ca = conditions.get(i);
                if (ca.destination == null || ca.destination.isBlank())
                    throw new IllegalHermesProcess("NULL or BLANK arch Destination in Condition Arch.");

                if ((i == 0 || i < size - 1) && (ca.condition == null || ca.condition.isBlank()))
                        throw new IllegalHermesProcess("NULL or BLANK Condition.");
            }
        }
        else {
            if (destination == null || destination.isBlank())
                throw new IllegalHermesProcess("NULL or BLANK arch Destination.");
        }
    }

    public record ConditionArch(String destination, String condition) {}
}
