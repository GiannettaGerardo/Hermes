package gg.hermes.model;

import gg.hermes.tasks.Arch;
import gg.hermes.tasks.Task;

import java.util.List;

public record HermesProcess(
        List<Task> nodes,
        List<Arch> arches,
        String startingNodeId
) {
}
