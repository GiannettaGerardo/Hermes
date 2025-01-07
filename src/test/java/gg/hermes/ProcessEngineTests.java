package gg.hermes;

import gg.hermes.engine.HermesGraph;
import gg.hermes.engine.HermesGraphFactory;
import gg.hermes.nodes.HermesTask;
import gg.hermes.testutility.ProcessesUtility;
import gg.hermes.testutility.TestLog;
import io.github.jamsesso.jsonlogic.JsonLogicConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessEngineTests
{
    private static final JsonLogicConfiguration jsonLogicConfiguration = new JsonLogicConfiguration();

    public static int nextTasks(HermesGraph graph, Map<String, Object> variables) {
        List<HermesTask> nextNodes = graph.getCurrentTasks();
        return graph.completeTask(nextNodes.get(0).getId(), variables);
    }

    @Test
    public void testOnlyConditionNoDataGoodEnding() throws Exception
    {
        HermesProcess process = ProcessesUtility.get("only-condition-no-data");
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
        HermesProcess process = ProcessesUtility.get("only-condition-with-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.BAD_ENDING, res);
    }

    @Test
    public void testConditionAndForwardWithDataBadEnding() throws Exception
    {
        HermesProcess process = ProcessesUtility.get("condition-and-forward-with-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        final int res = nextTasks(graph, Collections.singletonMap("my-value", 1));

        Assert.assertEquals(HermesGraph.BAD_ENDING, res);
    }

    @Test
    public void testLockRejectedWhenIncorrectTaskIdxParameter() throws Exception
    {
        HermesProcess process = ProcessesUtility.get("only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        int res = graph.completeTask(1000, null);

        Assert.assertEquals(HermesGraph.LOCK_REJECTED, res);
    }

    @Test
    public void testCompleteEndingTaskResultsInGoodEnding() throws Exception
    {
        HermesProcess process = ProcessesUtility.get("only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        nextTasks(graph, null);
        nextTasks(graph, null);

        final int res = graph.completeTask(3, null);

        Assert.assertEquals(HermesGraph.GOOD_ENDING, res);
    }

    @Test
    public void tesGetCurrentTasksAfterEndingResultsInEmptyList() throws Exception
    {
        HermesProcess process = ProcessesUtility.get("only-condition-no-data");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);

        nextTasks(graph, null);
        nextTasks(graph, null);

        final List<HermesTask> nextNodes = graph.getCurrentTasks();

        Assert.assertTrue(nextNodes.isEmpty());
    }

    @Test
    public void testSimpleForkJoinShouldWork() throws Exception
    {
        HermesProcess process = ProcessesUtility.get("simple-fork-join");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        List<HermesTask> nextNodes;
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
        HermesProcess process = ProcessesUtility.get("stalemate-fork-join-forward");
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        List<HermesTask> nextNodes;
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

    private void forLoopGenericTest(HermesProcess process) throws Exception
    {
        HermesGraph graph = HermesGraphFactory.getConcurrentGraph(process, jsonLogicConfiguration, TestLog::new);
        int result;
        int i = 0;
        List<HermesTask> nextNodes = graph.getCurrentTasks();
        Map<String, Object> data = new HashMap<>();
        data.put("i", i);

        for (; i < 3; ++i) {
            data.replace("i", i);
            result = graph.completeTask(nextNodes.get(0).getId(), data);
            Assert.assertEquals(HermesGraph.SUCCESS, result);
            nextNodes = graph.getCurrentTasks();
            Assert.assertEquals(1, nextNodes.size());
            Assert.assertEquals(0, nextNodes.get(0).getId());
        }
        data.replace("i", i);
        result = graph.completeTask(nextNodes.get(0).getId(), data);
        Assert.assertEquals(HermesGraph.GOOD_ENDING, result);
    }

    @Test
    public void testSimpleForLoop() throws Exception {
        forLoopGenericTest(ProcessesUtility.get("for-loop-1"));
    }

    @Test
    public void testForwardForLoop() throws Exception {
        forLoopGenericTest(ProcessesUtility.get("for-loop-2"));
    }
}
