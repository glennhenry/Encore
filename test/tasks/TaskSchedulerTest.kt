package tasks

import FakeTimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import server.core.network.TestConnection
import server.tasks.CancellationReason
import server.tasks.TaskScheduler
import server.tasks.ServerTaskDispatcher
import server.tasks.TaskConfig
import server.tasks.TaskName
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Test for default implementation of [TaskScheduler]: [ServerTaskDispatcher].
 *
 * Also demonstrate how to register, run, and stop a task.
 *
 * Runs on real timer with runBlocking.
 */
@Ignore("slow, real-time timer")
class TaskSchedulerTest {
    private val PID = "playerId123"
    private val NAME = "PlayerABC"

    @Test
    fun `test runTaskFor adds task to the running instances`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = ExampleTask(
                inputBlock = {
                    taskId = "task1"
                    parameter = "paramA"
                },
                stopBlock = {
                    taskId = "task1"
                }
            )
        )

        assertTrue(
            dispatcher.getAllRunningTaskFor(playerId = PID)
                .find { it.name == TaskName.DummyName.code } != null
        )
    }

    @Test
    fun `test stopTaskFor removes from the running instances`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = ExampleTask(
                inputBlock = {
                    taskId = "task1"
                    parameter = "paramA"
                },
                stopBlock = {
                    taskId = "task1"
                }
            )
        )

        dispatcher.stopTask<ExampleTaskStopParameter>(
            connection = connection,
            name = TaskName.DummyName,
            forceComplete = false,
            stopBlock = {
                taskId = taskId
            }
        )

        assertNull(
            dispatcher.getAllRunningTaskFor(playerId = PID)
                .find { it.name == TaskName.DummyName.code }
        )
    }

    @Test
    fun `test onStart should be fired immediately after runTaskFor`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        val task = ExampleTask(
            inputBlock = {
                taskId = "task1"
                parameter = "paramA"
            },
            stopBlock = {
                taskId = "task1"
            }
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = task
        )

        delay(50)
        assertEquals(1, task.state.onStartCount)

        dispatcher.stopTask<ExampleTaskStopParameter>(
            connection = connection,
            name = TaskName.DummyName,
            forceComplete = false,
            stopBlock = {
                taskId = taskId
            }
        )
    }

    @Test
    fun `test onExecute should be fired after start delay`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        val task = ExampleTask(
            inputBlock = {
                taskId = "task1"
                parameter = "paramA"
            },
            stopBlock = {
                taskId = "task1"
            }
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = task
        )

        delay(50)
        assertNotEquals(1, task.state.executeCount)

        delay(3050)
        assertEquals(1, task.state.executeCount)

        dispatcher.stopTask<ExampleTaskStopParameter>(
            connection = connection,
            name = TaskName.DummyName,
            forceComplete = false,
            stopBlock = {
                taskId = taskId
            }
        )
    }

    @Test
    fun `test onIterationStart and onIterationComplete fired on each iteration`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        val task = ExampleTask(
            inputBlock = {
                taskId = "task1"
                parameter = "paramA"
            },
            stopBlock = {
                taskId = "task1"
            }
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = task
        )

        delay(50)
        assertNotEquals(1, task.state.onIterationStartCount)
        assertNotEquals(1, task.state.onIterationCompleteCount)

        delay(3050)
        assertEquals(1, task.state.onIterationStartCount)
        assertEquals(1, task.state.onIterationCompleteCount)

        dispatcher.stopTask<ExampleTaskStopParameter>(
            connection = connection,
            name = TaskName.DummyName,
            forceComplete = false,
            stopBlock = {
                taskId = taskId
            }
        )
    }

    @Test
    fun `test onIterationStart and onIterationComplete shouldn't be fired for non-repeating task`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        val task = ExampleTask(
            inputBlock = {
                taskId = "task1"
                parameter = "paramA"
            },
            stopBlock = {
                taskId = "task1"
            }
        )
        task.config = TaskConfig(repeatInterval = null, maxRepeats = null)

        dispatcher.runTask(
            connection = connection,
            taskToRun = task
        )

        delay(50)
        delay(3050)
        assertEquals(0, task.state.onIterationStartCount)
        assertEquals(0, task.state.onIterationCompleteCount)

        dispatcher.stopTask<ExampleTaskStopParameter>(
            connection = connection,
            name = TaskName.DummyName,
            forceComplete = false,
            stopBlock = {
                taskId = taskId
            }
        )
    }

    @Test
    fun `test stopTaskFor properly call onForceComplete or onCancelled`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        val task = ExampleTask(
            inputBlock = {
                taskId = "task1"
                parameter = "paramA"
            },
            stopBlock = {
                taskId = "task1"
            }
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = task
        )

        delay(50)

        dispatcher.stopTask<ExampleTaskStopParameter>(
            connection = connection,
            name = TaskName.DummyName,
            forceComplete = false,
            stopBlock = {
                taskId = taskId
            }
        )

        delay(50)
        assertEquals(1, task.state.onCancelledCount)
        assertEquals(0, task.state.onForceCompleteCount)
        assertEquals(0, task.state.onTaskCompleteCount)
        assertEquals(CancellationReason.Manual, task.state.cancellationReason)
    }

    @Test
    fun `test the complete lifecycle of a task`() = runBlocking {
        val time = FakeTimeProvider(0)
        val dispatcher = ServerTaskDispatcher(time)
        val connection = createConnection(this)

        dispatcher.registerTask(
            name = TaskName.DummyName,
            stopFactory = { ExampleTaskStopParameter() },
            deriveTaskId = { playerId: String, name, _ ->
                // "DN-pid123-unit
                "${name.code}-$playerId-unit"
            }
        )

        val task = ExampleTask(
            inputBlock = {
                taskId = "task1"
                parameter = "paramA"
            },
            stopBlock = {
                taskId = "task1"
            }
        )

        task.config = TaskConfig(
            startDelay = 1.seconds,
            repeatInterval = 2.seconds,
            maxRepeats = 2
        )

        dispatcher.runTask(
            connection = connection,
            taskToRun = task
        )

        delay(50)
        assertEquals(1, task.state.onStartCount)

        delay(2050)
        assertEquals(1, task.state.onIterationStartCount)
        assertEquals(1, task.state.executeCount)
        assertEquals(1, task.state.onIterationCompleteCount)

        delay(2050)
        assertEquals(2, task.state.onIterationStartCount)
        assertEquals(2, task.state.executeCount)
        assertEquals(2, task.state.onIterationCompleteCount)

        assertEquals(1, task.state.onTaskCompleteCount)
    }

    private fun createConnection(scope: CoroutineScope): TestConnection {
        return TestConnection(
            playerId = PID,
            playerName = NAME,
            connectionScope = scope
        )
    }
}
