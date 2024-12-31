package gg.hermes;

import gg.hermes.exception.IllegalHermesProcess;
import gg.hermes.testutility.ModularHermesProcess;
import gg.hermes.testutility.ProcessesUtility;
import org.junit.After;
import org.junit.Test;

public class HermesProcessTests
{
    public void processValidation(String processId, String archesId) {
        ModularHermesProcess mhp = ProcessesUtility.get(processId);
        HermesProcess process = new HermesProcess(mhp.nodes(), mhp.arches().get(archesId), mhp.startingNodeId());
        process.validate();
    }

    @After
    public void printNewLine() {
        System.out.println();
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testGoodProcess1WithForkWithoutJoinShouldPass() throws Exception
    {
        System.out.println("* TEST: testGoodProcess1WithForkWithoutJoinShouldPass");
        processValidation("simple-process-1", "hermes-process-good-1");
    }

    @Test(expected = IllegalHermesProcess.class)
    public void testGraphArchWithLevelAndForkChangeShouldThrowException() throws Exception
    {
        System.out.println("* TEST: testGraphArchWithLevelAndForkChangeShouldThrowException");
        processValidation("simple-process-1", "hermes-process-bad-1");
    }

    @Test(expected = IllegalHermesProcess.class)
    public void testGraphArchWithForkChangeShouldThrowException() throws Exception
    {
        System.out.println("* TEST: testGraphArchWithForkChangeShouldThrowException");
        processValidation("simple-process-1", "hermes-process-bad-2");
    }
}
