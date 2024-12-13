package gg.hermes;

import gg.hermes.engine.HermesGraph;
import gg.hermes.engine.HermesGraphFactory;
import gg.hermes.tasks.ITask;
import gg.hermes.testutility.ProcessesUtility;
import gg.hermes.testutility.TestLog;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ProcessEngineTests {
    private static final JsonLogicConfiguration jsonLogicConfiguration = new JsonLogicConfiguration();

    public static int nextTasks(HermesGraph graph) {
        List<ITask> nextNodes = graph.getCurrentTasks();
        System.out.println(nextNodes.toString());
        return graph.completeTask(nextNodes.get(0).getIdx());
    }

    @Test
    public void testSimpleProcess1GoodEnding() throws Exception {
        int res = 9999999;
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(ProcessesUtility.get("simple-process-1"), jsonLogicConfiguration, TestLog::new);

        for (int i = 0; i < 2; ++i)
        {
            res = nextTasks(graph);
        }
        Assert.assertEquals(HermesGraph.RESULT_GOOD_ENDING, res);
    }
}
