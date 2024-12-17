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

    public static HermesProcess newProcess(String processName, String archesModuleName) {
        ModularHermesProcess mhp = ProcessesUtility.get(processName);
        return new HermesProcess(mhp.nodes(), mhp.arches().get(archesModuleName), mhp.startingNodeId());
    }

    @After
    public void printNewLine() {
        System.out.println();
    }

    @Test
    public void testSimpleProcessOnlyConditionNoDataGoodEnding() throws Exception
    {
        System.out.println("TEST: testSimpleProcessOnlyConditionNoDataGoodEnding");
        HermesProcess process = newProcess("simple-process-1", "only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        int res = 999;

        for (int i = 0; i < 2; ++i) {
            res = nextTasks(graph, null);
        }

        Assert.assertEquals(HermesGraph.GOOD_ENDING, res);
    }

    @Test
    public void testSimpleProcessOnlyConditionWithDataBadEnding() throws Exception
    {
        System.out.println("TEST: testSimpleProcessOnlyConditionWithDataBadEnding");
        HermesProcess process = newProcess("simple-process-1", "only-condition-with-data");
        var node = process.getNodes().get(0);
        node.setNumberOfVariables(1);
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        node.setNumberOfVariables(null);

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.BAD_ENDING, res);
    }

    @Test
    public void testSimpleProcessConditionAndForwardWithDataBadEnding() throws Exception
    {
        System.out.println("TEST: testSimpleProcessConditionAndForwardWithDataBadEnding");
        HermesProcess process = newProcess("simple-process-1", "condition-and-forward-with-data");
        var node = process.getNodes().get(0);
        node.setNumberOfVariables(1);
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        node.setNumberOfVariables(null);

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.BAD_ENDING, res);
    }
}
