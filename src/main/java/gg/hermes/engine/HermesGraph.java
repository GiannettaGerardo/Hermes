package gg.hermes.engine;

import gg.hermes.tasks.ITask;

import java.util.List;

public interface HermesGraph
{
    int RESULT_GOOD_ENDING = -10;
    int RESULT_BAD_ENDING = -11;
    int RESULT_LOCK_REJECTED = 0;
    int RESULT_OK = 1;
    int RESULT_NO_MOVE = 2;
    int RESULT_INVALID_RESOLVE = 3;

    List<ITask> getCurrentTasks();
    int completeTask(int taskIdx);
}
