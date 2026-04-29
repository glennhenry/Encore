import encore.acts.ActConcept
import encore.acts.StageAct
import encore.acts.photocard.PhotocardSubunit
import encore.acts.photocard.model.ActProgress
import encore.acts.setup.ActSetup
import encore.acts.setup.LifetimeMode
import encore.acts.setup.PerformMode
import encore.fancam.Fancam
import encore.utils.Ids
import encore.utils.SystemTime
import encore.utils.TimeProvider
import io.ktor.util.date.*
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import testHelper.TestFancam
import testHelper.VirtualTimeProvider
import java.text.SimpleDateFormat
import kotlin.math.floor
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

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
        runTimer(3000, this)
    }

    @Test
    fun `bound once multiple acts`() = runTest {
        // all these acts should start instantly since
        // director.run launches a coroutine and returns the act ID immediately

        // In real environment, there may be slight delay caused by
        // CPU, GC, coroutine, or setup creation

        // inside the coroutine, onStart is called, delay is started, perform is called,
        // and finally onEndingFairy is called
        runTimer(3000L, this)  // finish at tick 3s
        runTimer(3000L, this)  // finish at tick 3s
        runTimer(3000L, this)  // finish at tick 3s
        runTimer(2000L, this)  // finish at tick 2s
        runTimer(3500L, this)  // finish at tick 3.5s
        runTimer(3001L, this)  // finish at tick 3.001s
        runTimer(60000L, this) // finish at tick 60s
    }

    @Test
    fun `isActive works as expected`() = runTest {
        val director = StageActDirector(
            PhotocardSubunit.createForTest(),
            VirtualTimeProvider(this)
        )

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3000) {},
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
            concept = TimerActConcept(3100) {
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
        val director = StageActDirector(PhotocardSubunit.createForTest(), SystemTime)

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3000) {
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
            PhotocardSubunit.createForTest(),
            VirtualTimeProvider(this)
        )

        // create the error act
        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(5000) {
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
            concept = TimerActConcept(5001) {
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
        // 3000, 5000, 7000, 9000, 11000, 13000
        runRepeatTimer(3000L, 5, 2000L, this) {}
    }

    @Test
    fun `bound repeat stop successfully stops it`() = runBlocking {
        val director = StageActDirector(PhotocardSubunit.createForTest(), SystemTime)
        var performCount = 0

        val id = director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(500, 500, 3) { count ->
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


    private fun runTimer(time: Long, scope: TestScope): String {
        val director = StageActDirector(
            PhotocardSubunit.createForTest(),
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(time) {
                Fancam.trace { "TestScope.currentTime=${scope.currentTime}" }
                assertEquals(scope.currentTime, time)
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
        initialDelay: Long, repetition: Int,
        interval: Long, scope: TestScope, assertHook: ((Int) -> Unit)?
    ): String {
        val director = StageActDirector(
            PhotocardSubunit.createForTest(),
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(initialDelay, interval, repetition) { performNumber ->
                Fancam.trace { "[run=$performNumber] TestScope.currentTime=${scope.currentTime}" }
                assertEquals(scope.currentTime, initialDelay + (performNumber - 1) * interval)
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
}

data class TimerActConcept(
    val delay: Long,
    val onPerform: (Int) -> Unit
) : ActConcept

class TimerAct : StageAct<TimerActConcept> {
    override val name: String = "TimerAct"

    override fun createId(concept: TimerActConcept): String {
        return Ids.uuid()
    }

    override fun createSetup(concept: TimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.delay,
            performMode = PerformMode.Once,
            lifetimeMode = LifetimeMode.Bound
        )
    }

    override suspend fun perform(concept: TimerActConcept, performNumber: Int, batch: Int) {
        concept.onPerform(performNumber)
    }
}

data class RepeatTimerActConcept(
    val initialDelay: Long,
    val interval: Long,
    val repetition: Int,
    val onPerform: (Int) -> Unit
) : ActConcept

class RepeatTimerAct : StageAct<RepeatTimerActConcept> {
    override val name: String = "RepeatTimerAct"

    override fun createId(concept: RepeatTimerActConcept): String {
        return Ids.uuid()
    }

    override fun createSetup(concept: RepeatTimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Repeat(concept.repetition, concept.interval),
            lifetimeMode = LifetimeMode.Bound
        )
    }

    override suspend fun perform(concept: RepeatTimerActConcept, performNumber: Int, batch: Int) {
        concept.onPerform(performNumber)
    }
}

data class Step(
    val delay: Long,
    val runs: Int,
    val isFinished: Boolean
)

class StageActDirector(
    private val photocardSubunit: PhotocardSubunit,
    private val timeProvider: TimeProvider
) {
    private val boundChoreo: BoundChoreographer = BoundChoreographer(timeProvider)
    private val activeActs = mutableMapOf<String, Job>()

    fun <T : ActConcept> run(act: StageAct<T>, concept: T, scope: ActScope): String {
        val setup = act.createSetup(concept)
        val id = act.createId(concept)

        val job = scope.coroutineScope.launch {
            var performCount = 0
            val startedAt = timeProvider.now()

            try {
                act.onStart(concept)

                while (true) {
                    val step = boundChoreo.nextStep(setup, startedAt, performCount)

                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    act.perform(concept, performCount + step.runs, step.runs)
                    performCount += step.runs

                    if (step.isFinished) break
                }

                act.onEndingFairy(concept)
            } catch (_: CancellationException) {
                Fancam.trace { "Act '${act.name}' is cancelled for ${scope.ownerId}." }

                when (setup.lifetimeMode) {
                    is LifetimeMode.Bound -> {}
                    is LifetimeMode.PausedPersistent -> {}
                    is LifetimeMode.ContinuousPersistent -> {}
                }

            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for ${scope.ownerId}." }
            } finally {
                activeActs.remove(id)
            }
        }

        activeActs[id] = job
        return id
    }

    fun stop(actId: String): Boolean {
        (activeActs[actId] ?: return false)
            .cancel(CancellationException("Stop called"))
        return true
    }

    fun isActive(actId: String): Boolean {
        return activeActs.containsKey(actId)
    }
}

interface ActScope {
    val ownerId: String
    val coroutineScope: CoroutineScope
}

/*
val id = act.createId(input)
        val identity = act.createIdentity(input)
        val startedNow = getTimeMillis()
        val photocard = Photocard(
            actId = id,
            name = act.name,
            progress = ActProgress(
                startedAt = startedNow,
                accumulatedDelay = 0,
                lastActiveAt = startedNow,
                performCount = 0
            ),
            data = identity
        )
 */

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

interface Choreographer {
    fun nextStep(setup: ActSetup, startedAt: Long, performCount: Int): Step
}

class BoundChoreographer(private val timeProvider: TimeProvider) : Choreographer {
    override fun nextStep(setup: ActSetup, startedAt: Long, performCount: Int): Step {
        val delay = delayForNextPerform(setup, startedAt, performCount)
        return Step(delay, 1, isFinished(setup, performCount + 1))
    }

    fun delayForNextPerform(setup: ActSetup, startedAt: Long, performCount: Int): Long {
        if (performCount <= 0) {
            return setup.initialDelay
        }

        when (setup.performMode) {
            is PerformMode.Once -> {
                throw IllegalArgumentException(
                    "nextPerformAt called on PerformMode.Once when " +
                            "performCount is already $performCount."
                )
            }

            is PerformMode.Repeat -> {
                if (performCount >= setup.performMode.repetition + 1) {
                    throw IllegalArgumentException(
                        "nextPerformAt called on PerformMode.Repeat when " +
                                "performCount is already $performCount (repeat=${setup.performMode.repetition})."
                    )
                }

                val nextPerformAt = nextPerformAt(
                    startedAt, setup.initialDelay,
                    performCount, setup.performMode.interval
                )
                val now = timeProvider.now()
                val timeLeftUntilNextPerform = nextPerformAt - now
                return timeLeftUntilNextPerform
            }

            is PerformMode.Forever -> {
                val nextPerformAt = nextPerformAt(
                    startedAt, setup.initialDelay,
                    performCount, setup.performMode.interval
                )
                val now = timeProvider.now()
                val timeLeftUntilNextPerform = nextPerformAt - now
                return timeLeftUntilNextPerform
            }
        }
    }

    private fun nextPerformAt(
        startedAt: Long, initialDelay: Long,
        performCount: Int, interval: Long
    ): Long {
        val firstPerformAt = startedAt + initialDelay
        val nextPerformIndex = performCount + 1
        val nextPerformAt = firstPerformAt + (nextPerformIndex - 1) * interval
        return nextPerformAt
    }

    fun isFinished(setup: ActSetup, newPerformCount: Int): Boolean {
        return when (setup.performMode) {
            is PerformMode.Once -> true
            is PerformMode.Repeat -> {
                newPerformCount == setup.performMode.repetition + 1
            }

            is PerformMode.Forever -> false
        }
    }
}

class StageActChoreographer {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    fun performsTodo(firstPerformAt: Long, performCount: Int, performMode: PerformMode): Int {
        val now = getTimeMillis()

        when (performMode) {
            is PerformMode.Once -> return 1
            is PerformMode.Forever -> {
                val expectedPerformCountNow =
                    floor((now - firstPerformAt).toDouble() / performMode.interval.toDouble()).toInt() + 1
                val missedPerformCount = expectedPerformCountNow - performCount
                val remainingPerformCount = Int.MAX_VALUE - performCount
                val performsTodo = minOf(missedPerformCount, remainingPerformCount)
                return performsTodo
            }

            is PerformMode.Repeat -> {
                val expectedPerformCountNow =
                    floor((now - firstPerformAt).toDouble() / performMode.interval.toDouble()).toInt() + 1
                val missedPerformCount = expectedPerformCountNow - performCount
                val remainingPerformCount = performMode.repetition - performCount
                val performsTodo = minOf(missedPerformCount, remainingPerformCount)
                return performsTodo
            }
        }
    }

    fun nextPerformAt(setup: ActSetup, progress: ActProgress, performCount: Int): Long {
        if (performCount <= 0) {
            return setup.initialDelay
        }

        when (setup.performMode) {
            is PerformMode.Once -> {
                Fancam.warn {
                    "nextPerformAt called on PerformMode.Once when performCount is $performCount. " +
                            "Returned ${setup.initialDelay} (${dateFormatter.format(setup.initialDelay)})"
                }
                return setup.initialDelay
            }

            is PerformMode.Repeat -> {
                val firstPerformAt = progress.startedAt + setup.initialDelay
                val nextPerformIndex = performCount + 1
                val nextPerformAt = firstPerformAt + (nextPerformIndex - 1) * setup.performMode.interval

                if (performCount == setup.performMode.repetition - 1) {
                    Fancam.warn {
                        "nextPerformAt called on PerformMode.Repeat when performCount is $performCount (repeat=${setup.performMode.repetition}). " +
                                "Returned $nextPerformAt (${dateFormatter.format(nextPerformAt)})"
                    }
                }

                return nextPerformAt
            }

            is PerformMode.Forever -> {
                val firstPerformAt = progress.startedAt + setup.initialDelay
                val nextPerformIndex = performCount + 1
                val nextPerformAt = firstPerformAt + (nextPerformIndex - 1) * setup.performMode.interval

                return nextPerformAt
            }
        }
    }

    fun delayForNextPerform(setup: ActSetup, progress: ActProgress): Long {
        return nextPerformAt(setup, progress, progress.performCount) - progress.accumulatedDelay
    }
}
