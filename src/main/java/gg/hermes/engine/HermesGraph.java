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

    /**
     * Returns currently active tasks to complete.
     * Never returns tasks of type {@code FORWARD}, {@code JOIN} and {@code ENDING}.
     * @return currently active tasks.
     */
    List<ITask> getCurrentTasks();

    /**
     * Complete the task with id {@code taskId} and advance through the process.
     * @param taskId the task id of a current task.
     * @return a code that represent the result:
     * <ul>
     *     <li>{@code LOCK_REJECTED}: the attempt to obtain the lock for this task id failed;</li>
     *     <li>{@code SUCCESS}: the task has been completed and the process has advanced;</li>
     *     <li>{@code INVALID_VARIABLES}: 'variables' is invalid or the task with
     *     the given id does not allow variables;</li>
     *     <li>{@code STALEMATE_ENDING}: the process did not terminate with an ENDING task but is stuck in a state
     *     from which it is no longer possible to exit. The process construction should probably be revised;</li>
     *     <li>{@code GOOD_ENDING}: the process ended with a good ENDING task;</li>
     *     <li>{@code BAD_ENDING}: the process ended with a bad ENDING task.</li>
     * </ul>
     */
    int completeTask(int taskId);

    /**
     * Complete the task with id {@code taskId} and advance through the process.
     * @param taskId the task id of a current task.
     * @param variables variables to save for this task (if the task allows them).
     * @return a code that represent the result:
     * <ul>
     *     <li>{@code LOCK_REJECTED}: the attempt to obtain the lock for this task id failed;</li>
     *     <li>{@code SUCCESS}: the task has been completed and the process has advanced;</li>
     *     <li>{@code INVALID_VARIABLES}: 'variables' is invalid or the task with
     *     the given id does not allow variables;</li>
     *     <li>{@code STALEMATE_ENDING}: the process did not terminate with an ENDING task but is stuck in a state
     *     from which it is no longer possible to exit. The process construction should probably be revised;</li>
     *     <li>{@code GOOD_ENDING}: the process ended with a good ENDING task;</li>
     *     <li>{@code BAD_ENDING}: the process ended with a bad ENDING task.</li>
     * </ul>
     */
    int completeTask(int taskId, Map<String, Object> variables);
}
