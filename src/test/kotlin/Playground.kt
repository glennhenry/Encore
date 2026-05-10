import encore.acts.ActConcept
import encore.acts.ActIdStore
import encore.acts.ActScope
import encore.acts.StageAct
import encore.acts.choreo.BasicChoreography
import encore.acts.choreo.Choreography
import encore.acts.choreo.ChoreographyContext
import encore.acts.choreo.PerformMode
import encore.fancam.Fancam
import encore.utils.Ids
import encore.utils.SystemTime
import encore.utils.TimeProvider
import encore.utils.safelySuspend
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import testHelper.TestFancam
import testHelper.VirtualTimeProvider
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Playground for quick testing and code run
 *
 * .\gradlew test --tests "Playground.playground" --console=plain
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Playground {
    @BeforeTest
    fun setup() {
        TestFancam.create()
    }

    @Test
    fun `act run once`() = runTest {
        runTimer(3.seconds, this)
    }

    @Test
    fun `multiple acts run once`() = runTest {
        // all these acts should start instantly since
        // director.run launches a coroutine and returns the act ID immediately

        // In real environment, there may be slight delay caused by
        // CPU, GC, coroutine, or setup creation

        // inside the coroutine, onStart is called, delay is started, perform is called,
        // and finally onEndingFairy is called
        runTimer(3.seconds, this)          // finish at tick 3s
        runTimer(3.seconds, this)          // finish at tick 3s
        runTimer(3.seconds, this)          // finish at tick 3s
        runTimer(2.seconds, this)          // finish at tick 2s
        runTimer(3500.milliseconds, this)  // finish at tick 3.5s
        runTimer(3001.milliseconds, this)  // finish at tick 3.001s
        runTimer(60.seconds, this)         // finish at tick 60s
    }

    private fun runTimer(time: Duration, scope: TestScope): String {
        val director = StageActDirector(
            VirtualTimeProvider(scope),
            ActIdStore()
        )

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(time) {
                Fancam.trace { "TestScope.currentTime=${scope.currentTime}" }
                assertEquals(scope.currentTime.milliseconds, time)
            },
            scope = ActScope("TestScope", scope)
        )

        Fancam.trace { "Timer started (will fire after ${time}ms)." }
        return id
    }

    @Test
    fun `isActive works as expected`() = runTest {
        val director = StageActDirector(
            VirtualTimeProvider(this),
            ActIdStore()
        )

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3.seconds) {},
            scope = ActScope("TestScope", this)
        )

        // must be active after run is called
        val isActive = director.isRunning(id)
        assertTrue(isActive)
        Fancam.trace { "Timer started and isActive=${true}" }

        // assert that the act is no longer active after it finishes
        // runTest skips any delay and can't wait 3 seconds before this assertation
        // rely on another timer which finishes later to assert
        director.run(
            act = TimerAct(),
            concept = TimerActConcept(3100.milliseconds) {
                val isActive = director.isRunning(id)
                assertFalse(isActive)
                Fancam.trace { "isActive=${false}" }
            },
            scope = ActScope("TestScope", this)
        )
    }

    @Test
    fun `act stop successfully stops it`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3.seconds) {
                throw AssertionError("Executed here after 3 secs")
            },
            scope = ActScope("TestScope", this)
        )

        val isActive = director.isRunning(id)
        assertTrue(isActive)

        // stop and wait cancellation to finish
        assertTrue(director.stop(id))
        advanceUntilIdle()

        val isActive2 = director.isRunning(id)
        assertFalse(isActive2)
    }

    @Test
    fun `act throw error should stop it`() = runTest {
        val director = StageActDirector(
            VirtualTimeProvider(this),
            ActIdStore()
        )

        // create the error act
        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(5.seconds) {
                throw RuntimeException("Exception inside perform")
            },
            scope = ActScope("TestScope", this)
        )

        // rely on another timer to assert
        // the error act should already throws before this
        director.run(
            act = TimerAct(),
            concept = TimerActConcept(5001.milliseconds) {
                // assert the error act is no longer active
                // log should also have error message
                val isActive = director.isRunning(id)
                assertFalse(isActive)
                Fancam.trace { "isActive=${false}" }
            },
            scope = ActScope("TestScope", this)
        )
    }

    @Test
    fun `act run repeat`() = runTest {
        val director = StageActDirector(
            VirtualTimeProvider(this),
            ActIdStore()
        )
        val initialDelay = 3.seconds
        val interval = 2.seconds

        director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(initialDelay, 5, interval) { performNumber ->
                Fancam.trace { "[run=$performNumber] TestScope.currentTime=${this.currentTime}" }
                assertEquals(
                    this.currentTime,
                    initialDelay.inWholeMilliseconds + (performNumber - 1) * interval.inWholeMilliseconds
                )
            },
            scope = ActScope("TestScope", this)
        )

        Fancam.trace { "Repeat timer started (will fire after ${initialDelay}ms, every ${interval}ms for 1 + 5 repeats)." }
    }

    @Test
    fun `act repeat stop successfully stops it`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        var performCount = 0

        val id = director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(500.milliseconds, 3, 500.milliseconds) { count ->
                Fancam.trace { "[run=$count] (${500 + (count - 1) * 500}ms)" }
                performCount += 1
            },
            scope = ActScope("TestScope", this)
        )

        // rely on another timer to stop the task at certain point
        director.run(
            act = TimerAct(),
            concept = TimerActConcept(1700.milliseconds) {
                assertTrue(director.stop(id))
            },
            scope = ActScope("TestScope", this)
        )

        advanceUntilIdle()
        assertEquals(3, performCount)
    }

    @Test
    fun `act runs forever`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        var performCount = 0

        val interval = 2.seconds
        director.run(
            act = ForeverTimerAct(),
            concept = ForeverTimerActConcept(Duration.ZERO, interval) { performNumber ->
                Fancam.trace { "[run=$performNumber] TestScope.currentTime=${this.currentTime}" }
                assertEquals(
                    this.currentTime,
                    0L + (performNumber - 1) * interval.inWholeMilliseconds
                )
                performCount += 1

                // stopping timer without director.stop by throwing CancellationException
                if (performNumber == 50) {
                    assertEquals(50, performCount)
                    throw CancellationException("Manual stop")
                }
            },
            scope = ActScope("TestScope", this)
        )

        Fancam.trace { "Forever timer started (will fire after 0ms, every ${interval.inWholeMilliseconds}ms)." }
    }

    @Test
    fun `act with slow onStart shouldn't drift execution`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        val scope = ActScope("TestScope", this)

        director.run(
            TimerWithOnStartAct(),
            TimerWithOnStartConcept(
                3.seconds,
                onStart = { delay(1.seconds) },
                onPerform = { assertEquals(this.currentTime, 3000L) }),
            scope
        )
    }

    @Test
    fun `act should be cancellable during non-perform lifecycle`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        val scope = ActScope("TestScope", this)

        // onStart very slow
        val id = director.run(
            TimerWithOnStartAct(),
            TimerWithOnStartConcept(
                delay = 3.seconds,
                onStart = { delay(5.seconds) },
                onPerform = { }),
            scope
        )

        // should start immediately and cancel the first act after one second.
        director.run(
            TimerWithOnStartAct(),
            TimerWithOnStartConcept(
                delay = 1.seconds,
                onStart = {},
                onPerform = {
                    assertTrue(director.stop(id))
                    advanceUntilIdle()
                    assertFalse(director.isRunning(id))
                }),
            scope
        )
    }

    @Test
    fun `director should unbind actId after task is stopped`() = runTest {
        val store = ActIdStore()
        val director = StageActDirector(SystemTime, store)
        val scope = ActScope("TestScope", this)

        val id = director.run(
            TimerAct(),
            TimerActConcept(
                delay = 3.seconds,
                onPerform = { }),
            scope
        )
        store.bind(id, "hello123")

        director.run(
            TimerAct(),
            TimerActConcept(
                delay = 1.seconds,
                onPerform = {
                    // should be same for finish or error since we use finally block
                    assertNotNull(store.find("hello123"))
                    assertTrue(director.stop(id))
                    advanceUntilIdle()
                    assertFalse(director.isRunning(id))
                    assertNull(store.find("hello123"))
                }),
            scope
        )
    }

    @Test
    fun `runContinue skips onStart`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        val scope = ActScope("TestScope", this)

        director.runContinue(
            TimerWithOnStartAct(),
            TimerWithOnStartConcept(
                delay = 1.seconds,
                onStart = { throw AssertionError("onStart was executed") },
                onPerform = { }),
            scope
        )
    }

    @Test
    fun `executeAndContinue skips onStart and executes directly`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        val scope = ActScope("TestScope", this)

        director.performAndContinue(
            TimerWithOnStartAct(),
            TimerWithOnStartConcept(
                delay = 1.seconds,
                onStart = { throw AssertionError("onStart was executed") },
                onPerform = { assertEquals(0, currentTime) }),
            scope
        )
    }

    @Test
    fun `executeAndContinue skips onStart, executes directly, and continue normal flow`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        val scope = ActScope("TestScope", this)

        director.performAndContinue(
            RepeatTimerWithOnStartAct(),
            RepeatTimerWithOnStartConcept(
                initialDelay = 1.seconds,
                repeat = 3,
                interval = 1.seconds,
                onStart = { throw AssertionError("onStart was executed") },
                onPerform = { count ->
                    // with executeAndContinue,
                    // run would be 0, 1, 2, 3
                    // instead of 1, 2, 3, 4
                    assertEquals(1000L * (count - 1), currentTime)
                }),
            scope
        )
    }
}

fun StageActDirector.runTimer(duration: Duration, scope: ActScope, block: suspend () -> Unit): String {
    return run(
        act = TimerAct(),
        concept = TimerActConcept(duration) {
            block()
        },
        scope = scope
    )
}

fun StageActDirector.runRepeatingTimer(
    initialDelay: Duration, repetition: Int, interval: Duration,
    scope: ActScope,
    block: suspend (Int) -> Unit
): String {
    return run(
        act = RepeatTimerAct(),
        concept = RepeatTimerActConcept(initialDelay, repetition, interval) {
            block(it)
        },
        scope = scope
    )
}

fun StageActDirector.runForeverTimer(
    initialDelay: Duration, interval: Duration,
    scope: ActScope,
    block: suspend (Int) -> Unit
): String {
    return run(
        act = ForeverTimerAct(),
        concept = ForeverTimerActConcept(initialDelay, interval) {
            block(it)
        },
        scope = scope
    )
}


data class TimerWithOnStartConcept(
    val delay: Duration,
    val onStart: suspend () -> Unit,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class TimerWithOnStartAct : StageAct<TimerWithOnStartConcept> {
    override val name: String = "TimerWithOnStartAct"

    override suspend fun onStart(concept: TimerWithOnStartConcept) {
        concept.onStart()
    }

    override fun choreography(concept: TimerWithOnStartConcept): Choreography<TimerWithOnStartConcept> {
        return BasicChoreography(
            initialDelay = concept.delay,
            performMode = PerformMode.Once
        )
    }

    override suspend fun perform(concept: TimerWithOnStartConcept, performNumber: Int) {
        concept.onPerform(performNumber)
    }
}

data class RepeatTimerWithOnStartConcept(
    val initialDelay: Duration,
    val repeat: Int,
    val interval: Duration,
    val onStart: suspend () -> Unit,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class RepeatTimerWithOnStartAct : StageAct<RepeatTimerWithOnStartConcept> {
    override val name: String = "RepeatTimerWithOnStartAct"

    override suspend fun onStart(concept: RepeatTimerWithOnStartConcept) {
        concept.onStart()
    }

    override fun choreography(concept: RepeatTimerWithOnStartConcept): Choreography<RepeatTimerWithOnStartConcept> {
        return BasicChoreography(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Repeat(concept.repeat, concept.interval)
        )
    }

    override suspend fun perform(concept: RepeatTimerWithOnStartConcept, performNumber: Int) {
        concept.onPerform(performNumber)
    }
}

data class TimerActConcept(
    val delay: Duration,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class TimerAct : StageAct<TimerActConcept> {
    override val name: String = "TimerAct"

    override fun choreography(concept: TimerActConcept): Choreography<TimerActConcept> {
        return BasicChoreography(
            initialDelay = concept.delay,
            performMode = PerformMode.Once
        )
    }

    override suspend fun perform(concept: TimerActConcept, performNumber: Int) {
        concept.onPerform(performNumber)
    }
}

data class RepeatTimerActConcept(
    val initialDelay: Duration,
    val repetition: Int,
    val interval: Duration,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class RepeatTimerAct : StageAct<RepeatTimerActConcept> {
    override val name: String = "RepeatTimerAct"

    override fun choreography(concept: RepeatTimerActConcept): Choreography<RepeatTimerActConcept> {
        return BasicChoreography(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Repeat(concept.repetition, concept.interval)
        )
    }

    override suspend fun perform(concept: RepeatTimerActConcept, performNumber: Int) {
        concept.onPerform(performNumber)
    }
}

data class ForeverTimerActConcept(
    val initialDelay: Duration,
    val interval: Duration,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class ForeverTimerAct : StageAct<ForeverTimerActConcept> {
    override val name: String = "ForeverTimerAct"

    override fun choreography(concept: ForeverTimerActConcept): Choreography<ForeverTimerActConcept> {
        return BasicChoreography(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Forever(concept.interval)
        )
    }

    override suspend fun perform(concept: ForeverTimerActConcept, performNumber: Int) {
        concept.onPerform(performNumber)
    }
}

/**
 * Manage and provide entry point to run [StageAct].
 *
 * #### Usage
 *
 * - [run] to start a new stage act in normal behavior.
 * - [runContinue] to run a stage act in a continuation context which will
 *   skip the [StageAct.onStart] lifecycle and continue running in normal behavior.
 * - [performAndContinue] to run a stage act and call perform directly.
 *   This will also skip the [StageAct.onStart] lifecycle and continue running
 *   in normal behavior.
 *
 * Any of the call will return while also starting the act immediately.
 * Each will also produce a unique runtime identifier of the stage act.
 *
 * #### Lifecycle
 *
 * During normal behavior, a newly started act will:
 * - call [StageAct.onStart].
 * - enters execution loop and waits for the delay returned by [Choreography.next].
 * - call [StageAct.perform].
 * - repeat the process until repetition is finished.
 * - call [StageAct.onEndingFairy] on finish.
 *
 * In other circumstances:
 * - call [StageAct.onCancelled] whenever the act is stopped via [stop],
 *   which may happen manually or from conditions like process shutdown,
 *   player disconnect, etc.
 * - call [StageAct.onError] whenever any error was thrown throughout its
 *   lifecycle or logic.
 *
 * #### Cancellation
 *
 * A running act can be cancelled via [stop]. This will cancel the running
 * coroutine with a `CancellationException`.
 *
 * During act's execution or lifecycle, a cancellation can happen in mid-execution.
 * Due to this, implementations may protect critical sections using
 * `withContext(NonCancellable)` to ensure atomic execution of important
 * operations such as persistence updates or consistency-sensitive state changes.
 *
 * Stage act can also cancel itself by throwing `CancellationException` within
 * its lifecycle.
 *
 * ##### Identity
 *
 * Running a stage act returns an act identifier (UUID). Stopping a stage act
 * requires this unique identifier. Application may use component like [ActIdStore]
 * to store the ID and retrieve it using another identifier which the application
 * can derive themself (e.g., derivable with input from [ActConcept]).
 *
 * #### Continuation
 *
 * The stage act system acts solely as a runtime scheduler and executor.
 * It does not automatically persist or resume unfinished acts.
 *
 * Continuation is handled explicitly by the application by:
 * - persisting the required progress data, and
 * - re-running the act with updated scheduling information.
 *
 * This is typically achieved by [ActConcept] taking data which is
 * then used by [StageAct.choreography] to derive the execution's timing.
 *
 * ##### Progress persistence
 *
 * Stage act implementations may persist progress data within their
 * lifecycle hooks. For instance:
 * - saving `actFinishedAt`
 * - storing remaining duration
 * - marking finished state
 *
 * During player's reconnection or application recovery, the application may:
 * - load the persisted progress data,
 * - reconstruct the corresponding act instance, and
 * - re-run the act with the updated scheduling logic.
 *
 * When error occur, [StageAct.onError] will be called and act may:
 * - mark state as invalid
 * - delete persisted progress data
 * - notify client
 *
 * @property timeProvider Component to provide time. Use [SystemTime] for real use.
 * @property actStore Provides storage and access for running act identifiers.
 */
class StageActDirector(
    private val timeProvider: TimeProvider,
    private val actStore: ActIdStore
) {
    private val activeActs = mutableMapOf<String, Job>()

    /**
     * Entry point to run a new stage act.
     *
     * @param act The instance of [StageAct] to be run.
     * @param concept Runtime input of the stage act.
     * @param scope Runtime boundary of stage act.
     * @param T The type of [concept].
     * @return A unique runtime identifier of the stage act.
     */
    fun <T : ActConcept> run(act: StageAct<T>, concept: T, scope: ActScope): String {
        return launchAct(act, concept, scope, callOnStart = true, performDirectly = false)
    }

    /**
     * Run a stage act in resumpsion context which skips the [StageAct.onStart] lifecycle.
     *
     * @param act The instance of [StageAct] to be run.
     * @param concept Runtime input of the stage act.
     * @param scope Runtime boundary of stage act.
     * @param T The type of [concept].
     * @return A unique runtime identifier of the stage act.
     */
    fun <T : ActConcept> runContinue(act: StageAct<T>, concept: T, scope: ActScope): String {
        return launchAct(act, concept, scope, callOnStart = false, performDirectly = false)
    }

    /**
     * Run a stage act in a catch-up context which will call [StageAct.perform]
     * directly. After that, it will continue running without calling [StageAct.onStart]
     * lifecycle if it hasn't finish yet.
     *
     * @param act The instance of [StageAct] to be run.
     * @param concept Runtime input of the stage act.
     * @param scope Runtime boundary of stage act.
     * @param T The type of [concept].
     * @return A unique runtime identifier of the stage act.
     */
    fun <T : ActConcept> performAndContinue(act: StageAct<T>, concept: T, scope: ActScope): String {
        return launchAct(act, concept, scope, callOnStart = false, performDirectly = true)
    }

    /**
     * Main scheduling code of stage act.
     *
     * By calling this, a stage act will be scheduled to run immediately.
     * It will also return a unique runtime identifier of the stage act.
     */
    private fun <T : ActConcept> launchAct(
        act: StageAct<T>,
        concept: T,
        scope: ActScope,
        callOnStart: Boolean,
        performDirectly: Boolean
    ): String {
        val startedAt = timeProvider.now()
        val id = Ids.uuid()
        val choreo = act.choreography(concept)

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var performCount = 0
            var firstPerformAt: Long? = null
            var previousPerformAt: Long? = null
            var finished = false

            try {
                if (callOnStart) {
                    act.onStart(concept)
                }

                if (performDirectly) {
                    val now = timeProvider.now()
                    firstPerformAt = now
                    previousPerformAt = now
                    act.perform(concept, 1)
                    performCount = 1
                }

                while (true) {
                    val now = timeProvider.now()
                    val delay = choreo.next(
                        concept = concept,
                        context = ChoreographyContext(
                            currentMillis = now,
                            performCount = performCount,
                            startedAt = startedAt,
                            firstPerformAt = firstPerformAt,
                            previousPerformAt = previousPerformAt,
                        )
                    )
                    previousPerformAt = now

                    if (delay == null) break
                    if (delay > 0) delay(delay.milliseconds)
                    if (firstPerformAt == null) firstPerformAt = timeProvider.now()

                    act.perform(concept, ++performCount)
                }

                finished = true
                act.onEndingFairy(concept)
            } catch (_: CancellationException) {
                if (!finished) {
                    safelySuspend {
                        act.onCancelled(concept)
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'" }
                safelySuspend {
                    act.onError(concept, e)
                }
            } finally {
                activeActs.remove(id)
                actStore.unbind(id)
            }
        }

        activeActs[id] = job
        return id
    }

    /**
     * Stop the running stage act identified by [actId].
     *
     * This will cancel the running coroutine even if it still
     * run or in mid-execution.
     *
     * @param actId Unique identifier of the running stage act.
     * @return `false` when act is not found, otherwise `true`.
     */
    fun stop(actId: String?): Boolean {
        (activeActs[actId] ?: return false)
            .cancel(CancellationException("Act was stopped"))
        return true
    }

    /**
     * Returns whether the stage act identified by [actId] is running.
     *
     * A stage act is considered as running when the associated act's
     * coroutine job is in an internal data structure.
     */
    fun isRunning(actId: String): Boolean {
        return activeActs.containsKey(actId)
    }
}
