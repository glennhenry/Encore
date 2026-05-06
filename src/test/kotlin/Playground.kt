import encore.acts.*
import encore.acts.director.ActScope
import encore.acts.setup.ActSetup
import encore.acts.setup.PerformMode
import encore.fancam.Fancam
import encore.utils.DayOfWeek
import encore.utils.Ids
import encore.utils.SystemTime
import encore.utils.TimeOfDay
import encore.utils.TimeProvider
import encore.utils.nextOccurrence
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import testHelper.TestFancam
import testHelper.VirtualTimeProvider
import java.time.ZoneId
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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
    fun `bound once`() = runTest {
        runTimer(3.seconds, this)
    }

    @Test
    fun `bound once multiple acts`() = runTest {
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

    @Test
    fun `isActive works as expected`() = runTest {
        val director = StageActDirector(
            VirtualTimeProvider(this)
        )

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3.seconds) {},
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )

        // must be active after run is called
        val isActive = director.isActive(id)
        assertTrue(isActive)
        Fancam.trace { "Timer started and isActive=${true}" }

        // assert that the act is no longer active after it finishes
        // runTest skips any delay, so must rely on another timer to assert
        director.run(
            act = TimerAct(),
            concept = TimerActConcept(3100.milliseconds) {
                val isActive = director.isActive(id)
                assertFalse(isActive)
                Fancam.trace { "isActive=${false}" }
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope-reassert"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )
    }

    @Test
    fun `bound act stop successfully stops it`() = runBlocking {
        val director = StageActDirector(SystemTime)

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3.seconds) {
                throw AssertionError("Executed here after 3 secs")
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = this@runBlocking
            }
        )

        // must be active after run
        val isActive = director.isActive(id)
        assertTrue(isActive)
        Fancam.trace { "Timer started and isActive=${true}" }

        // must rely on real time because stop or job.cancel() takes some time
        // that can't be awaited with runTest
        delay(200.milliseconds)
        assertTrue(director.stop(id))
        delay(200.milliseconds)
        val isActive2 = director.isActive(id)
        assertFalse(isActive2)
        Fancam.trace { "isActive=${false}" }
    }

    @Test
    fun `bound act error gets terminated`() = runTest {
        val director = StageActDirector(
            VirtualTimeProvider(this)
        )

        // create the error act
        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(5.seconds) {
                throw RuntimeException("Exception inside perform")
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )

        // assert active
        val isActive = director.isActive(id)
        assertTrue(isActive)
        Fancam.trace { "Timer started and isActive=${true}" }

        // rely on another timer to assert
        // the error act should already throws before this
        director.run(
            act = TimerAct(),
            concept = TimerActConcept(5001.milliseconds) {
                // assert the error act is no longer active
                // log should also have error message
                val isActive = director.isActive(id)
                assertFalse(isActive)
                Fancam.trace { "isActive=${false}" }
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )
    }

    @Test
    fun `bound repeat`() = runTest {
        // 3s, 5s, 7s, 9s, 11s, 13s
        runRepeatTimer(3.seconds, 5, 2.seconds, this) {}
    }

    @Test
    fun `bound repeat stop successfully stops it`() = runBlocking {
        val director = StageActDirector(SystemTime)
        var performCount = 0

        val id = director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(500.milliseconds, 500.milliseconds, 3) { count ->
                Fancam.trace { "[run=$count] (${500 + (count - 1) * 500}ms)" }
                performCount += 1
            },
            scope = object : ActScope {
                override val ownerId: String = "Test"
                override val coroutineScope: CoroutineScope = this@runBlocking
            }
        )

        // must rely on real time because stop or job.cancel() takes some time
        // that can't be awaited with runTest
        // 500ms -> performCount = 1
        // 1000ms -> performCount = 2
        // 1500ms -> performCount = 3
        // 1700ms -> performCount = 3 (stopped)
        delay(1700.milliseconds)
        assertTrue(director.stop(id))
        assertEquals(3, performCount)
    }

    @Test
    fun `bound forever`() = runTest {
        var performCount = 0
        runForeverTimer(Duration.ZERO, 2.seconds, this) { count ->
            performCount += 1

            // stopping timer without director.stop by throwing CancellationException
            if (count == 50) {
                assertEquals(50, performCount)
                throw CancellationException("Manual stop")
            }
        }
    }

    private fun runTimer(time: Duration, scope: TestScope): String {
        val director = StageActDirector(
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(time) {
                Fancam.trace { "TestScope.currentTime=${scope.currentTime}" }
                assertEquals(scope.currentTime.milliseconds, time)
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = scope
            }
        )

        Fancam.trace { "Timer started (will fire after ${time}ms)." }
        return id
    }

    private fun runRepeatTimer(
        initialDelay: Duration, repetition: Int,
        interval: Duration, scope: TestScope, assertHook: ((Int) -> Unit)?
    ): String {
        val director = StageActDirector(
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(initialDelay, interval, repetition) { performNumber ->
                Fancam.trace { "[run=$performNumber] TestScope.currentTime=${scope.currentTime}" }
                assertEquals(
                    scope.currentTime,
                    initialDelay.inWholeMilliseconds + (performNumber - 1) * interval.inWholeMilliseconds
                )
                assertHook?.invoke(performNumber)
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = scope
            }
        )

        Fancam.trace { "Repeat timer started (will fire after ${initialDelay}ms, every ${interval}ms for 1 + $repetition repeats)." }
        return id
    }

    private fun runForeverTimer(
        initialDelay: Duration, interval: Duration,
        scope: TestScope, assertHook: ((Int) -> Unit)?
    ): String {
        val director = StageActDirector(
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = ForeverTimerAct(),
            concept = ForeverTimerActConcept(initialDelay, interval) { performNumber ->
                Fancam.trace { "[run=$performNumber] TestScope.currentTime=${scope.currentTime}" }
                assertEquals(
                    scope.currentTime,
                    initialDelay.inWholeMilliseconds + (performNumber - 1) * interval.inWholeMilliseconds
                )
                assertHook?.invoke(performNumber)
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = scope
            }
        )

        Fancam.trace { "Forever timer started (will fire after ${initialDelay}ms, every ${interval}ms)." }
        return id
    }
}

data class TimerActConcept(
    val delay: Duration,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class TimerAct : StageAct<TimerActConcept> {
    override val name: String = "TimerAct"

    override fun createId(concept: TimerActConcept): String {
        return Ids.uuid()
    }

    override fun createSetup(concept: TimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.delay,
            performMode = PerformMode.Once
        )
    }

    override suspend fun perform(concept: TimerActConcept, performNumber: Int, batch: Int) {
        concept.onPerform(performNumber)
    }
}

data class RepeatTimerActConcept(
    val initialDelay: Duration,
    val interval: Duration,
    val repetition: Int,
    val onPerform: suspend (Int) -> Unit
) : ActConcept

class RepeatTimerAct : StageAct<RepeatTimerActConcept> {
    override val name: String = "RepeatTimerAct"

    override fun createId(concept: RepeatTimerActConcept): String {
        return Ids.uuid()
    }

    override fun createSetup(concept: RepeatTimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Repeat(concept.repetition, concept.interval)
        )
    }

    override suspend fun perform(concept: RepeatTimerActConcept, performNumber: Int, batch: Int) {
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

    override fun createId(concept: ForeverTimerActConcept): String {
        return Ids.uuid()
    }

    override fun createSetup(concept: ForeverTimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Forever(concept.interval)
        )
    }

    override suspend fun perform(concept: ForeverTimerActConcept, performNumber: Int, batch: Int) {
        concept.onPerform(performNumber)
    }
}

data class Step(
    val delay: Long,
    val runsTodo: Int,
    val isFinished: Boolean
)

class StageActDirector(private val timeProvider: TimeProvider) {
    private val boundChoreo = BoundChoreography(timeProvider)
    private val activeActs = mutableMapOf<String, Job>()

    fun <T : ActConcept> run(act: StageAct<T>, concept: T, scope: ActScope): String {
        val id = act.createId(concept)
        val setup = act.createSetup(concept)

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val firstPerformAt = timeProvider.now() + setup.initialDelay.toLong(DurationUnit.MILLISECONDS)
            var performCount = 0
            var finished = false

            try {
                act.onStart(concept)

                while (true) {
                    val step = boundChoreo.nextStep(setup, firstPerformAt, performCount)

                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    act.perform(concept, performCount + 1, 1)
                    performCount += 1

                    if (step.isFinished) break
                }

                finished = true
                act.onEndingFairy(concept)
            } catch (_: CancellationException) {
                // cancel reason is either stopped or killed
                // since bound act "cannot" be killed, to guarantee correctness
                // CancellationReason.Stopped is passed directly
                if (!finished) {
                    withContext(NonCancellable) {
                        act.onCancelled(concept, CancellationReason.Stopped)
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
            } finally {
                activeActs.remove(id)
            }
        }

        activeActs[id] = job
        return id
    }

    fun stop(actId: String): Boolean {
        (activeActs[actId] ?: return false)
            .cancel(StopCancellationException())
        return true
    }

    fun isActive(actId: String): Boolean {
        return activeActs.containsKey(actId)
    }
}

class BoundChoreography(private val timeProvider: TimeProvider) {
    fun nextStep(setup: ActSetup, firstPerformAt: Long, performCount: Int): Step {
        val delay = delayLeft(setup, firstPerformAt, performCount)
        return Step(delay, 1, isFinished(setup, performCount + 1))
    }

    private fun delayLeft(setup: ActSetup, firstPerformAt: Long, performCount: Int): Long {
        return when (setup.performMode) {
            is PerformMode.Once -> {
                calculateDelay(firstPerformAt, performCount, Duration.ZERO)
            }

            is PerformMode.Repeat -> {
                calculateDelay(firstPerformAt, performCount, setup.performMode.interval)
            }

            is PerformMode.Forever -> {
                calculateDelay(firstPerformAt, performCount, setup.performMode.interval)
            }
        }
    }

    private fun calculateDelay(firstPerformAt: Long, performCount: Int, interval: Duration): Long {
        val nextPerformAt = firstPerformAt + performCount * (interval.inWholeMilliseconds)
        val timeLeftUntilNextPerform = nextPerformAt - timeProvider.now()
        return timeLeftUntilNextPerform
    }

    private fun isFinished(setup: ActSetup, newPerformCount: Int): Boolean {
        return when (setup.performMode) {
            is PerformMode.Once -> true
            is PerformMode.Repeat -> {
                newPerformCount == setup.performMode.repetition + 1
            }

            is PerformMode.Forever -> false
        }
    }
}

val SystemTimezone: ZoneId = ZoneId.systemDefault()

/**
 * Defines how a [StageAct] is scheduled over time.
 *
 * This determines *when* an act should perform, but not *what* it does.
 *
 * Implementations:
 * - [BasicChoreography]: Covers simple runtime scheduling based on an initial delay
 *   and a fixed perform pattern (once, repeat, or forever).
 * - [DailyChoreography]: Schedules perform at a fixed [TimeOfDay] every day.
 * - [WeeklyChoreography]: Schedules perform at a fixed [DayOfWeek] and [TimeOfDay].
 *
 * For more advanced or dynamic scheduling behavior, implement [CustomChoreography].
 */
interface Choreography

/**
 * Basic scheduling configuration for a [StageAct].
 *
 * This model is suitable for simple tasks that:
 * - start after a fixed delay, and
 * - optionally repeat with a fixed interval.
 *
 * @property initialDelay The delay before the first performs.
 * @property performMode Defines the act performs portion.
 */
data class BasicChoreography(
    val initialDelay: Duration, val performMode: PerformMode
) : Choreography

/**
 * Advanced scheduling model for a [StageAct].
 *
 * Implementations define their own time model by computing the next perform
 * timestamp dynamically.
 *
 * This is useful for:
 * - context-dependent schedules (e.g. player state)
 * - dynamic intervals
 * - non-uniform or staged perform patterns
 *
 * @param T The [ActConcept] associated with the [StageAct].
 */
interface CustomChoreography<T : ActConcept> : Choreography {
    /**
     * Computes the next perform time.
     *
     * The returned value must be an absolute timestamp (epoch milliseconds),
     * not a delay. This ensures correct behavior across restarts and time shifts.
     *
     * @param currentPerformCount Number of times the act has already performed.
     * @param concept The act input used to derive scheduling decisions.
     * @param currentMillis The current time in epoch milliseconds.
     *
     * @return The next perform timestamp, or `null` if no further performs should occur.
     */
    fun next(currentPerformCount: Int, concept: T, currentMillis: Long): Long?
}

/**
 * Schedules a [StageAct] to perform once per day at a fixed [TimeOfDay].
 *
 * Perform is aligned to wall-clock time and repeats indefinitely
 * (unless explicitly stopped).
 *
 * @param T The [ActConcept] associated with the [StageAct].
 * @property runAt The time of day at which the act should perform.
 */
data class DailyChoreography<T : ActConcept>(
    val runAt: TimeOfDay
) : CustomChoreography<T> {
    override fun next(currentPerformCount: Int, concept: T, currentMillis: Long): Long {
        return runAt.nextOccurrence(currentMillis, SystemTimezone)
    }
}

/**
 * Schedules a [StageAct] to perform once per week at a specific
 * [DayOfWeek] and [TimeOfDay].
 *
 * Perform is aligned to wall-clock time and repeats indefinitely
 * (unless explicitly stopped).
 *
 * @param T The [ActConcept] associated with the [StageAct].
 * @property runAtDay The day of the week on which the act should perform.
 * @property runAtTime The time of day at which the act should perform.
 */
data class WeeklyChoreography<T : ActConcept>(
    val runAtDay: DayOfWeek,
    val runAtTime: TimeOfDay
)

/*
Time model and illustration

initialDelay = 5s
interval     = 10s
repetition   = 3

0      5    9      15         25    30    35
S -----!----T------!----------!-----N-----!
    5  ! 4     6   !    10    !  5     5  !

startedAt        : S = 0
accumulatedDelay : T = 9
performCount     : C = 1
now              : N = 30
lastActiveAt     : T = 9

This task already performs 1 time, where lastActive was at tick 9.
Now in tick 30.

expectedPerformCountNow = (now - firstPerformAt) / interval + 1          (3  = floor((30 - 5) / 10 + 1))
missedPerformCount      = expectedPerformCountNow - performCount         (2  = 3 - 1)
remainingPerformCount   = repetition - performCount                      (2  = 3 - 1)
performsTodo            = min(missedperformCount, remainingPerformCount) (2  = min(2, 2))

This means that the task need to be performed 2 repeats in batch to compensate the miss.

firstPerformAt          = startedAt + initialDelay                           (5  = 0 + 5)
nextPerformIndex        = performCount + 1                                   (2  = 1 + 1)
nextPerformAt           = firstPerformAt + (nextPerformIndex - 1) * interval (15 = 5 + (2 - 1) * 10)
oldRemainingDelay       = nextPerformAt - accumulatedDelay                   (6  = 15 - 9)

delayJump               = (nextPerformIndex - 1) * interval                                    (10 = 2 - 1 * 10)
inactiveTime            = now - lastActiveAt                                                   (21 = 30 - 9)
newPerformCount         = performCount + performsTodo                                          (3  = 1 + 2)
newRemainingDelay       = inactiveTime - oldRemainingDelay - delayJump                         (5  = 21 - 6 - 10)
newAccumulatedDelay     = accumulatedDelay + oldRemainingDelay + newRemainingDelay + delayJump (30 = 9 + 6 + 10 + 5)

The task then continues with the newRemainingDelay

With the pause model, no performs will be missed and the remainingDelay will still be 6.
*/
