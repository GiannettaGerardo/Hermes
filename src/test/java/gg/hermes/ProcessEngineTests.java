package gg.hermes;

import gg.hermes.engine.HermesGraph;
import gg.hermes.engine.HermesGraphFactory;
import gg.hermes.tasks.ITask;
import gg.hermes.testutility.ModularHermesProcess;
import gg.hermes.testutility.ProcessesUtility;
import gg.hermes.testutility.TestLog;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProcessEngineTests
{
    private static final JsonLogicConfiguration jsonLogicConfiguration = new JsonLogicConfiguration();

    public static int nextTasks(HermesGraph graph, Map<String, Object> variables) {
        List<ITask> nextNodes = graph.getCurrentTasks();
        System.out.println(nextNodes.toString());
        return graph.completeTask(nextNodes.get(0).getIdx(), variables);
    }

    public static HermesGraph newGraph(String processName, String archesModuleName) {
        ModularHermesProcess mhp = ProcessesUtility.get(processName);
        HermesProcess hp = new HermesProcess(mhp.nodes(), mhp.arches().get(archesModuleName), mhp.startingNodeId());
        return HermesGraphFactory.getConcurrentGraph(hp, jsonLogicConfiguration, TestLog::new);
    }

    @After
    public void printNewLine() {
        System.out.println();
    }

    @Test
    public void testSimpleProcessNoDataGoodEnding() throws Exception
    {
        System.out.println("TEST: testSimpleProcessNoDataGoodEnding");
        HermesGraph graph = newGraph("simple-process-1", "only-condition-no-data");
        int res = 999;

        for (int i = 0; i < 2; ++i) {
            res = nextTasks(graph, null);
        }

        Assert.assertEquals(HermesGraph.RESULT_GOOD_ENDING, res);
    }

    @Test
    public void testSimpleProcessWithDataBadEnding() throws Exception
    {
        System.out.println("TEST: testSimpleProcessWithDataBadEnding");
        HermesGraph graph = newGraph("simple-process-1", "only-condition-with-data");

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.RESULT_BAD_ENDING, res);
    }
}
