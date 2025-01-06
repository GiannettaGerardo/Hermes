package gg.hermes;

import gg.hermes.engine.HermesGraph;
import gg.hermes.engine.HermesGraphFactory;
import gg.hermes.tasks.ITask;
import gg.hermes.testutility.ModularHermesProcess;
import gg.hermes.testutility.ProcessesUtility;
import gg.hermes.testutility.TestLog;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
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
        return graph.completeTask(nextNodes.get(0).getId(), variables);
    }

    public static HermesProcess newProcess(String processName, String archesModuleName) {
        ModularHermesProcess mhp = ProcessesUtility.get(processName);
        return new HermesProcess(mhp.nodes(), mhp.arches().get(archesModuleName), mhp.startingNodeId());
    }

    @Test
    public void testOnlyConditionNoDataGoodEnding() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        int res = 0;

        for (int i = 0; i < 2; ++i) {
            res = nextTasks(graph, null);
        }

        Assert.assertEquals(HermesGraph.GOOD_ENDING, res);
    }

    @Test
    public void testOnlyConditionWithDataBadEnding() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "only-condition-with-data");
        var node = process.getNodes().get(0);
        node.setNumberOfVariables(1);
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        node.setNumberOfVariables(0);

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.BAD_ENDING, res);
    }

    @Test
    public void testConditionAndForwardWithDataBadEnding() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "condition-and-forward-with-data");
        var node = process.getNodes().get(0);
        node.setNumberOfVariables(1);
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        node.setNumberOfVariables(0);

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.BAD_ENDING, res);
    }

    @Test
    public void testLockRejectedWhenIncorrectTaskIdxParameter() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        int res = graph.completeTask(1000, null);

        Assert.assertEquals(HermesGraph.LOCK_REJECTED, res);
    }

    @Test
    public void testCompleteEndingTaskResultsInGoodEnding() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        nextTasks(graph, null);
        nextTasks(graph, null);

        final int res = graph.completeTask(3, null);

        Assert.assertEquals(HermesGraph.GOOD_ENDING, res);
    }

    @Test
    public void tesGetCurrentTasksAfterEndingResultsInEmptyList() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        nextTasks(graph, null);
        nextTasks(graph, null);

        final List<ITask> nextNodes = graph.getCurrentTasks();

        Assert.assertTrue(nextNodes.isEmpty());
    }

    @Test
    public void testSimpleForkJoinShouldWork() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "simple-fork-join");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        List<ITask> nextNodes;
        int result;

        // task 1
        nextNodes = graph.getCurrentTasks();
        result = graph.completeTask(nextNodes.get(0).getId());
        Assert.assertEquals(HermesGraph.SUCCESS, result);

        // tasks 2 and 7
        nextNodes = graph.getCurrentTasks();
        Assert.assertEquals(2, nextNodes.size());

        result = graph.completeTask(nextNodes.get(0).getId());
        Assert.assertEquals(HermesGraph.SUCCESS, result);

        // task 2 or 7
        nextNodes = graph.getCurrentTasks();
        Assert.assertEquals(1, nextNodes.size());

        result = graph.completeTask(nextNodes.get(0).getId());
        Assert.assertEquals(HermesGraph.SUCCESS, result);

        // tasks 4
        nextNodes = graph.getCurrentTasks();
        Assert.assertEquals(1, nextNodes.size());

        result = graph.completeTask(nextNodes.get(0).getId());
        Assert.assertEquals(HermesGraph.GOOD_ENDING, result);
    }

    @Test
    public void testStalemateShouldReturnStalemateEnding() throws Exception
    {
        HermesProcess process = newProcess("simple-process-1", "stalemate-fork-join-forward");
        var node = process.getNodes().get(6); // id_7
        node.setNumberOfVariables(1);
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        node.setNumberOfVariables(0);
        List<ITask> nextNodes;
        int result;

        // task 1
        nextNodes = graph.getCurrentTasks();
        result = graph.completeTask(nextNodes.get(0).getId());
        Assert.assertEquals(HermesGraph.SUCCESS, result);

        // task 7
        nextNodes = graph.getCurrentTasks();
        Assert.assertEquals(1, nextNodes.size());

        result = graph.completeTask(nextNodes.get(0).getId(), Collections.singletonMap("not_one", "two"));
        Assert.assertEquals(HermesGraph.STALEMATE_ENDING, result);
    }
}
