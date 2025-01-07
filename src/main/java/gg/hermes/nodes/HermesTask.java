package gg.hermes.nodes;

public interface HermesTask {
    int getId();
    HermesNodeType getType();
    String getName();
    String getDescription();

    default int getNumberOfVariables() {
        return 0;
    }

    default boolean isGoodEnding() {
        return false;
    }

    default int getArchesToJoin() {
        return 0;
    }
}
