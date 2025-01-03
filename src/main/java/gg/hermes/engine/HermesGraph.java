package gg.hermes.engine;

import gg.hermes.tasks.ITask;

import java.util.List;
import java.util.Map;

public interface HermesGraph
{
    int GOOD_ENDING = -10;
    int BAD_ENDING = -11;
    int LOCK_REJECTED = 0;
    int SUCCESS = 1;
    int INVALID_VARIABLES = 2;
    int STALEMATE_ENDING = 3;

    List<ITask> getCurrentTasks();
    int completeTask(int taskIdx);
    int completeTask(int taskIdx, Map<String, Object> variables);
}
