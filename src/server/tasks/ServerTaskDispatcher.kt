package server.tasks

import kotlinx.coroutines.*
import server.core.network.Connection
import utils.functions.SystemTime
import utils.functions.TimeProvider
import utils.logging.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Manages and dispatches task instances.
 *
 * This dispatcher is also a task scheduler (i.e., the default implementation of [TaskScheduler]).
 *
 * For usage, see:
 * - [registerTask]
 * - [runTask]
 * - [stopTask]
 *
 * @property runningInstances Map of task IDs to currently running [TaskInstance]s.
 * @property taskIdDerivers  Map of each [TaskName] string to a function capable of deriving a task ID
 *                           from a `playerId` and a generic [StopInput] type.
 *                           Every [ServerTask] implementation **must** call [registerTask] (in GameServer.kt)
 *                           to register how the dispatcher should compute a task ID for that category when stopping tasks.
 * @property stopTaskFactories Map of each [TaskName] string to a factory function that
 *                             creates a new instance of its corresponding `StopInput` type.
 */
class ServerTaskDispatcher(private val time: TimeProvider = SystemTime) : TaskScheduler {
    private val runningInstances = mutableMapOf<String, TaskInstance>()
    private val taskIdDerivers = mutableMapOf<String, (playerId: String, name: TaskName, stopInput: Any) -> String>()
    private val stopTaskFactories = mutableMapOf<String, () -> Any>()

    /**
     * Register the factory to stop task and a function that can derive
     * a task ID from a given task name.
     *
     * The [deriveTaskId] function takes a `String` of [Connection.playerId], the task name,
     * and a generic [StopInput] type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <StopInput : Any> registerTask(
        name: TaskName,
        stopFactory: () -> StopInput,
        deriveTaskId: (playerId: String, name: TaskName, stopInput: StopInput) -> String
    ) {
        stopTaskFactories[name.code] = stopFactory
        taskIdDerivers[name.code] = { playerId, name, stopInput ->
            try {
                deriveTaskId(playerId, name, stopInput as StopInput)
            } catch (e: ClassCastException) {
                val expectedType = stopInput::class.simpleName ?: "Unknown"
                val actualType = stopInput::class.simpleName ?: "Unknown"
                val msg = buildString {
                    appendLine("[registerTask] Type mismatch when deriving stop ID:")
                    appendLine("- Name: ${name.code}")
                    appendLine("- Expected: $expectedType; Actual: $actualType")
                    appendLine("- Registered TaskName(${name.code}) multiple times with different StopInput types.")
                    appendLine("- The stopInputFactory in registerTask() returns the wrong type (ensure non-unit return if the task is stoppable with some StopInput type)")
                    appendLine("- The task’s createStopInput() implementation doesn’t match the registered factory.")
                }

                throw IllegalStateException(msg, e)
            }
        }
    }

    /**
     * Run the selected [taskToRun] for the player's [Connection].
     *
     * The execution, timing, and delay logic depends on the task's config.
     * Stop the task using [stopTask].
     *
     * It's possible to construct a task, then save the task class reference somewhere.
     * This can be done if the caller need an access to some of task's internal state.
     */
    fun <TaskInput : Any, StopInput : Any> runTask(
        connection: Connection,
        taskToRun: ServerTask<TaskInput, StopInput>,
    ) {
        val stopInput = taskToRun.createStop().apply(taskToRun.stopBlock)

        val deriveStopId = taskIdDerivers[taskToRun.name.code]
            ?: error("stopIdProvider not registered for ${taskToRun.name.code} (use registerTask)")
        val taskId = deriveStopId(connection.playerId, taskToRun.name, stopInput)

        val job = connection.connectionScope.launch {
            try {
                Logger.info("runTask Hello") { "Task ${taskToRun.name.code} has been scheduled to run (waiting for startDelay) for playerId=${connection.playerId}, taskId=$taskId" }
                val scheduler = taskToRun.scheduler ?: this@ServerTaskDispatcher
                scheduler.schedule(connection, taskId, taskToRun)
            } catch (e: CancellationException) {
                when (e) {
                    is ForceCompleteException -> {
                        Logger.info("ForceCompleteException") { "Task '${taskToRun.name.code}' was forced to complete for playerId=${connection.playerId}, taskId=$taskId" }
                    }

                    is ManualCancellationException -> {
                        Logger.info("ManualCancellationException") { "Task '${taskToRun.name.code}' was manually cancelled for playerId=${connection.playerId}, taskId=$taskId" }
                    }

                    else -> {
                        Logger.warn("CancellationException") { "Task '${taskToRun.name.code}' was cancelled for playerId=${connection.playerId}, taskId=$taskId" }
                    }
                }
            } catch (e: Exception) {
                Logger.error("runTask Exception") { "Error on task '${taskToRun.name.code}': $e for playerId=${connection.playerId}, taskId=$taskId" }
            } finally {
                Logger.info("runTask Goodbye") { "Task '${taskToRun.name.code}' no longer run for playerId=${connection.playerId}, taskId=$taskId" }
                runningInstances.remove(taskId)
            }
        }

        runningInstances[taskId] = TaskInstance(
            name = taskToRun.name.code,
            playerId = connection.playerId,
            config = taskToRun.config,
            job = job
        )
    }

    /**
     * Default implementation of [TaskScheduler].
     *
     * The process at how specifically task lifecycle is handled is documented in [ServerTask].
     */
    @OptIn(InternalTaskAPI::class)
    override suspend fun <TaskInput : Any, StopInput : Any> schedule(
        connection: Connection,
        taskId: String,
        task: ServerTask<TaskInput, StopInput>
    ) {
        val config = task.config
        val shouldRepeat = config.repeatInterval != null
        var iterationDone = 0
        val startTime = time.now().toDuration(DurationUnit.MILLISECONDS)

        try {
            task.onStart(connection)
            delay(config.startDelay)

            Logger.info("[runTask Working]") { "Task '${task.name.code}' currently running for playerId=${connection.playerId}, taskId=$taskId" }

            if (shouldRepeat) {
                while (currentCoroutineContext().isActive) {
                    // Check timeout
                    config.timeout?.let { timeout ->
                        val now = time.now().toDuration(DurationUnit.MILLISECONDS)
                        if (now - startTime >= timeout) {
                            task.onCancelled(connection, CancellationReason.Timeout)
                            break
                        }
                    }

                    task.onIterationStart(connection)
                    task.execute(connection)
                    task.onIterationComplete(connection)

                    iterationDone++
                    // Check max repeat
                    config.maxRepeats?.let { max ->
                        if (iterationDone >= max) {
                            task.onTaskComplete(connection)
                            break
                        }
                    }

                    delay(config.repeatInterval)
                }
            } else {
                task.execute(connection)
                task.onTaskComplete(connection)
            }
        } catch (e: CancellationException) {
            when (e) {
                is ForceCompleteException -> task.onForceComplete(connection)

                is ManualCancellationException -> task.onCancelled(connection, CancellationReason.Manual)

                else -> {
                    task.onCancelled(connection, CancellationReason.Error)
                }
            }
            throw e
        } catch (e: Exception) {
            task.onCancelled(connection, CancellationReason.Error)
            throw e
        }
    }

    /**
     * Return all running tasks for [playerId].
     */
    fun getAllRunningTaskFor(playerId: String): List<TaskInstance> {
        return runningInstances.values.filter { it.playerId == playerId }
    }

    /**
     * Stop the task of [taskId] by cancelling the associated coroutine job.
     */
    private fun stopRunningTask(taskId: String) {
        runningInstances.remove(taskId)?.job?.cancel()
    }

    /**
     * Stop the task with the given [Connection.playerId], [category], and [StopInput].
     *
     * Depending on the [forceComplete]:
     * - If `forceComplete` is `true`, this will cause the task to fire the `onForceComplete` hook.
     * - If `forceComplete` is `false`, this will cause the task to fire the `onCancelled` hook.
     */
    @Suppress("UNCHECKED_CAST")
    fun <StopInput : Any> stopTask(
        connection: Connection,
        name: TaskName,
        forceComplete: Boolean = false,
        stopBlock: StopInput.() -> Unit = {}
    ) {
        val factory = stopTaskFactories[name.code]
            ?: error("No stopInputFactory registered for ${name.code} (use registerTask)")

        try {
            val stopInput = (factory() as StopInput).apply(stopBlock)

            val deriveTaskId = taskIdDerivers[name.code]
                ?: error("No stopIdProvider registered for ${name.code} (use registerTask)")

            val taskId = deriveTaskId(connection.playerId, name, stopInput)
            val instance = runningInstances.remove(taskId)

            if (instance == null) {
                Logger.warn("stopTaskFor") { "Instance for taskId=$taskId is null." }
                return
            }

            val exception = if (forceComplete) {
                ForceCompleteException()
            } else {
                ManualCancellationException()
            }

            instance.job.cancel(exception)
        } catch (e: ClassCastException) {
            val expectedType = stopBlock::class.simpleName ?: "Unknown"
            val msg = buildString {
                appendLine("[stopTaskFor] Type mismatch when casting factory stop ID:")
                appendLine("- Name: ${name.code}")
                appendLine("- Expected: $expectedType; Factory type : ${factory().javaClass.simpleName ?: "Unknown"}")
                appendLine("- The StopInput generic type used in stopTaskFor<${expectedType}> does not match the one registered via registerTask().")
            }

            throw IllegalStateException(msg, e)
        }
    }

    /**
     * Stop all tasks for the [playerId]
     */
    fun stopAllTasksForPlayer(playerId: String) {
        runningInstances
            .filterValues { it.playerId == playerId }
            .forEach { (taskId, _) -> stopRunningTask(taskId) }
    }

    /**
     * Stop every running tasks instances in the server.
     */
    fun stopAllPushTasks() {
        runningInstances.forEach { (taskId, _) -> stopRunningTask(taskId) }
    }

    fun shutdown() {
        stopAllPushTasks()
        runningInstances.clear()
        taskIdDerivers.clear()
    }
}

/**
 * An instance of task:
 * - identified by [name]
 * - intended for [playerId]
 * - with the config [TaskConfig]
 * - and coroutine reference [job]
 */
data class TaskInstance(
    val name: String,
    val playerId: String,
    val config: TaskConfig,
    val job: Job,
)

class ForceCompleteException : CancellationException("Force completion was requested")
class ManualCancellationException : CancellationException("Manual cancellation was done")
