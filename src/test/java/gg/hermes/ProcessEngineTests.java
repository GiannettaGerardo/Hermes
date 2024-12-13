package gg.hermes;

import com.google.gson.Gson;
import gg.hermes.engine.HermesGraph;
import gg.hermes.engine.HermesGraphFactory;
import gg.hermes.model.HermesProcess;
import gg.hermes.tasks.ITask;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProcessEngineTests {
    private static final Gson gson = new Gson();
    private static final JsonLogicConfiguration jsonLogicConfiguration = new JsonLogicConfiguration();

    public HermesProcess getProcess(final String fileName) throws IOException {
        try (
                InputStream inputStream = ClassLoader.getSystemResourceAsStream(fileName);
                Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        ) {
            return gson.fromJson(reader, HermesProcess.class);
        }
    }

    public static int nextTasks(HermesGraph graph) {
        List<ITask> nextNodes = graph.getCurrentTasks();
        System.out.println(nextNodes.toString());
        return graph.completeTask(nextNodes.get(0).getIdx());
    }

    @Test
    public void testSimpleProcess1GoodEnding() throws Exception {
        int res = 9999999;
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(getProcess("simple-process-1.json"), jsonLogicConfiguration, TestLog::new);

        for (int i = 0; i < 2; ++i)
        {
            res = nextTasks(graph);
        }
        Assert.assertEquals(HermesGraph.RESULT_GOOD_ENDING, res);
    }
}
