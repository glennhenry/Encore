package tasks

import server.core.network.Connection
import server.tasks.CancellationReason
import server.tasks.InternalTaskAPI
import server.tasks.ServerTask
import server.tasks.TaskConfig
import server.tasks.TaskName
import server.tasks.TaskScheduler
import utils.logging.Logger
import kotlin.time.Duration.Companion.seconds

/**
 * Example of a [ServerTask] implementation.
 *
 * This task start after 3 seconds and execute every 5 seconds for 3 times.
 * Input is a `taskId` and `parameter`, the stop identifier is the `taskId`.
 *
 * May want to pass `ServerContext` if the task need to update DB during task execution.
 */
class ExampleTask(
    override val inputBlock: ExampleTaskParameter.() -> Unit,
    override val stopBlock: ExampleTaskStopParameter.() -> Unit
) : ServerTask<ExampleTaskParameter, ExampleTaskStopParameter>() {
    override val name: TaskName = TaskName.DummyName
    override var config: TaskConfig = TaskConfig(startDelay = 3.seconds, repeatInterval = 5.seconds, maxRepeats = 3)
    override val scheduler: TaskScheduler? = null

    override fun createInput(): ExampleTaskParameter = ExampleTaskParameter()
    override fun createStop(): ExampleTaskStopParameter = ExampleTaskStopParameter()

    private val taskInput: ExampleTaskParameter by lazy {
        createInput().apply(inputBlock)
    }

    var state = TaskState()

    @InternalTaskAPI
    override suspend fun execute(connection: Connection) {
        Logger.debug("ExampleTask") { "execute() called; state=$state" }
        state = state.copy(executeCount = state.executeCount + 1)
    }

    @InternalTaskAPI
    override suspend fun onStart(connection: Connection) {
        Logger.debug("ExampleTask") { "onStart() called; input=$taskInput, state=$state" }
        state = state.copy(onStartCount = state.onStartCount + 1)
    }

    @InternalTaskAPI
    override suspend fun onIterationStart(connection: Connection) {
        Logger.debug("ExampleTask") { "onIterationStart() called; state=$state" }
        state = state.copy(onIterationStartCount = state.onIterationStartCount + 1)
    }

    @InternalTaskAPI
    override suspend fun onIterationComplete(connection: Connection) {
        Logger.debug("ExampleTask") { "onIterationComplete() called; state=$state" }
        state = state.copy(onIterationCompleteCount = state.onIterationCompleteCount + 1)
    }

    @InternalTaskAPI
    override suspend fun onCancelled(connection: Connection, reason: CancellationReason) {
        Logger.debug("ExampleTask") { "onCancelled() called with reason=$reason; state=$state" }
        state = state.copy(onCancelledCount = state.onCancelledCount + 1, cancellationReason = reason)
    }

    @InternalTaskAPI
    override suspend fun onForceComplete(connection: Connection) {
        Logger.debug("ExampleTask") { "onForceComplete() called; state=$state" }
        state = state.copy(onForceCompleteCount = state.onForceCompleteCount + 1)
    }

    @InternalTaskAPI
    override suspend fun onTaskComplete(connection: Connection) {
        Logger.debug("ExampleTask") { "onTaskComplete() called; state=$state" }
        state = state.copy(onTaskCompleteCount = state.onTaskCompleteCount + 1)
    }
}

data class ExampleTaskParameter(
    var taskId: String = "",
    var parameter: String = ""
)

data class ExampleTaskStopParameter(
    var taskId: String = ""
)

data class TaskState(
    val executeCount: Int = 0,
    val onStartCount: Int = 0,
    val onIterationStartCount: Int = 0,
    val onIterationCompleteCount: Int = 0,
    val onCancelledCount: Int = 0,
    val onForceCompleteCount: Int = 0,
    val onTaskCompleteCount: Int = 0,
    val cancellationReason: CancellationReason? = null
)
