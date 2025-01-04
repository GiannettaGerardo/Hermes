package gg.hermes.testutility;

import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;

import java.util.List;
import java.util.Map;

public record ModularHermesProcess(
        int startingNodeId,
        List<Task> nodes,
        Map<String, List<Arch>> arches
) {
}
