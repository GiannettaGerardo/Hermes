package gg.hermes.engine;

import gg.hermes.model.HermesProcess;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import org.slf4j.Logger;

import java.util.function.Function;

public final class HermesGraphFactory
{
    private HermesGraphFactory() {}

    public static HermesGraph getGraph(
            final HermesProcess hermesProcess,
            final JsonLogicConfiguration jsonLogicConf,
            final Function<Class<?>, Logger> implementLogger
    ) {
        return new HermesConcurrentGraph(
                hermesProcess,
                jsonLogicConf,
                implementLogger.apply(HermesConcurrentGraph.class)
        );
    }
}
