import encore.acts.ActConcept
import encore.acts.ActIdStore
import encore.acts.StageAct
import encore.acts.choreo.BasicChoreography
import encore.acts.choreo.Choreography
import encore.acts.choreo.ChoreographyContext
import encore.acts.choreo.PerformMode
import encore.acts.ActScope
import encore.fancam.Fancam
import encore.utils.Ids
import encore.utils.SystemTime
import encore.utils.TimeProvider
import encore.utils.safelySuspend
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
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
        val director = StageActDirector(SystemTime, ActIdStore())

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
        val director = StageActDirector(SystemTime, ActIdStore())
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
            var previousPerformAt: Long? = null
            var finished = false

            try {
                if (callOnStart) {
                    withContext(NonCancellable) {
                        act.onStart(concept)
                    }
                }

                if (performDirectly) {
                    val now = timeProvider.now()
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
                            previousPerformAt = previousPerformAt,
                            startedAt = startedAt,
                        )
                    )
                    previousPerformAt = now

                    if (delay == null) break
                    if (delay > 0) delay(delay.milliseconds)

                    act.perform(concept, ++performCount)
                }

                finished = true
                withContext(NonCancellable) {
                    act.onEndingFairy(concept)
                }
            } catch (_: CancellationException) {
                if (!finished) {
                    withContext(NonCancellable) {
                        safelySuspend {
                            act.onCancelled(concept)
                        }
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
                withContext(NonCancellable) {
                    safelySuspend {
                        act.onError(concept, e)
                    }
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
