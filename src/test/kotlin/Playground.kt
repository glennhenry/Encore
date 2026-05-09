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

    @Test
    fun `isActive works as expected`() = runTest {
        val director = StageActDirector(
            VirtualTimeProvider(this),
            ActIdStore()
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
        // runTest skips any delay and can't wait 3 seconds before this assertation
        // rely on another timer which finishes later to assert
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
    fun `act stop successfully stops it`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())

        val id = director.run(
            act = TimerAct(),
            concept = TimerActConcept(3.seconds) {
                throw AssertionError("Executed here after 3 secs")
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )

        val isActive = director.isActive(id)
        assertTrue(isActive)

        // stop and wait cancellation to finish
        assertTrue(director.stop(id))
        advanceUntilIdle()

        val isActive2 = director.isActive(id)
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
            scope = object : ActScope {
                override val ownerId: String = "TestScope"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )

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
    fun `act run repeat`() = runTest {
        runRepeatTimer(3.seconds, 5, 2.seconds, this) {}
    }

    @Test
    fun `act repeat stop successfully stops it`() = runTest {
        val director = StageActDirector(VirtualTimeProvider(this), ActIdStore())
        var performCount = 0

        val id = director.run(
            act = RepeatTimerAct(),
            concept = RepeatTimerActConcept(500.milliseconds, 500.milliseconds, 3) { count ->
                Fancam.trace { "[run=$count] (${500 + (count - 1) * 500}ms)" }
                performCount += 1
            },
            scope = object : ActScope {
                override val ownerId: String = "Test"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )

        // rely on another timer to stop the task at certain point
        director.run(
            act = TimerAct(),
            concept = TimerActConcept(1700.milliseconds) {
                assertTrue(director.stop(id))
            },
            scope = object : ActScope {
                override val ownerId: String = "Test"
                override val coroutineScope: CoroutineScope = this@runTest
            }
        )

        advanceUntilIdle()
        assertEquals(3, performCount)
    }

    @Test
    fun `act runs forever`() = runTest {
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
            VirtualTimeProvider(scope),
            ActIdStore()
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
            VirtualTimeProvider(scope),
            ActIdStore()
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
            VirtualTimeProvider(scope),
            ActIdStore()
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
    val interval: Duration,
    val repetition: Int,
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

class StageActDirector(
    private val timeProvider: TimeProvider,
    private val actStore: ActIdStore
) {
    private val activeActs = mutableMapOf<String, Job>()

    fun <T : ActConcept> run(act: StageAct<T>, concept: T, scope: ActScope): String {
        return launchAct(act, concept, scope, callOnStart = true, performDirectly = false)
    }

    fun <T : ActConcept> runContinue(act: StageAct<T>, concept: T, scope: ActScope): String {
        return launchAct(act, concept, scope, callOnStart = false, performDirectly = false)
    }

    fun <T : ActConcept> executeAndContinue(act: StageAct<T>, concept: T, scope: ActScope): String {
        return launchAct(act, concept, scope, callOnStart = false, performDirectly = true)
    }

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
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
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

    fun stop(actId: String?): Boolean {
        (activeActs[actId] ?: return false)
            .cancel(CancellationException("Act was stopped"))
        return true
    }

    fun isActive(actId: String): Boolean {
        return activeActs.containsKey(actId)
    }
}
