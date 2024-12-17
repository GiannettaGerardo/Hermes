package gg.hermes.tasks;

import gg.hermes.exception.IllegalHermesProcess;

public record Arch(
        String source,
        String destination,
        String condition
) {
    public void validate() {
        if (source == null || source.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK arch Source.");

        if (destination == null || destination.isBlank())
            throw new IllegalHermesProcess("NULL or BLANK arch Destination.");

        if (condition != null && condition.isBlank())
            throw new IllegalHermesProcess("BLANK arch Condition.");
    }
}
