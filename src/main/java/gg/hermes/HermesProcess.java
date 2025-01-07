package gg.hermes;

import gg.hermes.nodes.HermesNode;
import gg.hermes.nodes.HermesNodeType;

import java.util.*;

public class HermesProcess
{
    private List<HermesNode> process;

    private HermesProcess() {}

    public HermesProcess(List<HermesNode> process) {
        this.process = process;
    }

    public List<HermesNode> getProcess() {
        return process;
    }

    public void validate() {
        int size;
        int endingCounter = 0;
        if (process == null || (size = process.size()) == 0)
            throw new IllegalHermesProcess("NULL or EMPTY process list.");

        if (HermesNodeType.NORMAL != process.get(0).getType())
            throw new IllegalHermesProcess("Starting Node Type is NOT NORMAL.");

        for (int i = 0; i < size; ++i) {
            var task = process.get(i);
            if (task == null)
                throw new IllegalHermesProcess("Node " + i + " is NULL.");
            task.validate(i);
            if (task.getType() == HermesNodeType.ENDING)
                ++endingCounter;
        }

        if (endingCounter == 0)
            throw new IllegalHermesProcess("No ENDING nodes found.");

        checkArches();
    }

    private void checkArches() {
        List<Arch> arches;
        int archesSize;
        final int processSize = process.size();
        for (HermesNode node : process) {
            arches = node.getTo();
            if (arches == null)
                continue;
            if (HermesNodeType.ENDING == node.getType())
                throw new IllegalHermesProcess("An ENDING node cannot have an ARCH.");
            archesSize = arches.size();
            if (archesSize == 0)
                throw new IllegalHermesProcess("Invalid EMPTY Arch list in node " + node.getId());
            for (int i = 0; i < archesSize; ++i) {
                var arch = arches.get(i);
                if (arch == null)
                    throw new IllegalHermesProcess("Arch in index " + i + " is NULL.");
                arch.validate(processSize, i);
            }
        }
    }
}
