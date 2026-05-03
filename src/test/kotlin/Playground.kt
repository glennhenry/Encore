import encore.acts.*
import encore.acts.director.ActScope
import encore.acts.photocard.PhotocardSubunit
import encore.acts.photocard.model.ActProgress
import encore.acts.photocard.model.Photocard
import encore.acts.setup.ActSetup
import encore.acts.setup.LifetimeMode
import encore.acts.setup.PerformMode
import encore.datastore.collection.ServerId
import encore.fancam.Fancam
import encore.utils.Ids
import encore.utils.SystemTime
import encore.utils.TimeProvider
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import testHelper.TestFancam
import testHelper.VirtualTimeProvider
import kotlin.math.min
import kotlin.test.*
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

    @Test
    fun `bound forever`() = runTest {
        var performCount = 0
        runForeverTimer(0L, 2000L, this) { count ->
            performCount += 1

            // stopping timer without director.stop by throwing CancellationException
            if (count == 50) {
                assertEquals(50, performCount)
                throw CancellationException("Manual stop")
            }
        }
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

    private fun runForeverTimer(
        initialDelay: Long, interval: Long,
        scope: TestScope, assertHook: ((Int) -> Unit)?
    ): String {
        val director = StageActDirector(
            PhotocardSubunit.createForTest(),
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = ForeverTimerAct(),
            concept = ForeverTimerActConcept(initialDelay, interval) { performNumber ->
                Fancam.trace { "[run=$performNumber] TestScope.currentTime=${scope.currentTime}" }
                assertEquals(scope.currentTime, initialDelay + (performNumber - 1) * interval)
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


    private fun runPausedPersistentTimer(time: Long, identity: Map<String, String>, scope: TestScope): String {
        val director = StageActDirector(
            PhotocardSubunit.createForTest(),
            VirtualTimeProvider(scope)
        )

        val id = director.run(
            act = PausedPersistentTimerAct(),
            concept = PausedPersistentTimerActConcept(time, identity) {
                Fancam.trace { "TestScope.currentTime=${scope.currentTime} (identity=$identity)" }
                assertEquals(scope.currentTime, time)
            },
            scope = object : ActScope {
                override val ownerId: String = "TestScope-$identity"
                override val coroutineScope: CoroutineScope = scope
            }
        )

        Fancam.trace { "Paused persistent timer started (will fire after ${time}ms)." }
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

data class ForeverTimerActConcept(
    val initialDelay: Long,
    val interval: Long,
    val onPerform: (Int) -> Unit
) : ActConcept

class ForeverTimerAct : StageAct<ForeverTimerActConcept> {
    override val name: String = "ForeverTimerAct"

    override fun createId(concept: ForeverTimerActConcept): String {
        return Ids.uuid()
    }

    override fun createSetup(concept: ForeverTimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Forever(concept.interval),
            lifetimeMode = LifetimeMode.Bound
        )
    }

    override suspend fun perform(concept: ForeverTimerActConcept, performNumber: Int, batch: Int) {
        concept.onPerform(performNumber)
    }
}

data class PausedPersistentTimerActConcept(
    val initialDelay: Long,
    val identity: Map<String, String>,
    val onPerform: (Int) -> Unit
) : ActConcept

class PausedPersistentTimerAct : StageAct<PausedPersistentTimerActConcept> {
    override val name: String = "PausedPersistentTimerAct"

    override fun createId(concept: PausedPersistentTimerActConcept): String {
        return Ids.uuid()
    }

    override fun createIdentity(concept: PausedPersistentTimerActConcept): Map<String, String> {
        return concept.identity
    }

    override fun createSetup(concept: PausedPersistentTimerActConcept): ActSetup {
        return ActSetup(
            initialDelay = concept.initialDelay,
            performMode = PerformMode.Once,
            lifetimeMode = LifetimeMode.PausedPersistent
        )
    }

    override suspend fun perform(concept: PausedPersistentTimerActConcept, performNumber: Int, batch: Int) {
        concept.onPerform(performNumber)
    }
}

data class Step(
    val delay: Long,
    val runsTodo: Int,
    val isFinished: Boolean
)

class StageActDirector(
    private val photocardSubunit: PhotocardSubunit,
    private val timeProvider: TimeProvider
) {
    private val boundChoreo = BoundChoreography(timeProvider)
    private val pausedPersistentChoreo = PausedPersistentChoreography(timeProvider)
    private val continuousPersistentChoreo = ContinuousPersistentChoreography(timeProvider)

    private val activeActs = mutableMapOf<String, Job>()

    fun <T : ActConcept> run(act: StageAct<T>, concept: T, scope: ActScope): String {
        val setup = act.createSetup(concept)

        return when (setup.lifetimeMode) {
            is LifetimeMode.Bound -> runBound(act, setup, concept, scope)
            is LifetimeMode.PausedPersistent -> runPausedPersistent(act, setup, concept, scope)
            is LifetimeMode.ContinuousPersistent -> runContinuousPersistent(act, setup, concept, scope)
        }
    }

    fun <T : ActConcept> resume(act: StageAct<T>, concept: T, photocard: Photocard, scope: ActScope): String {
        if (isActive(photocard.actId)) {
            Fancam.warn { "Can't resume an already running act; id=${photocard.actId}, name=${photocard.name}." }
        }

        val setup = act.createSetup(concept)

        return when (setup.lifetimeMode) {
            is LifetimeMode.Bound -> {
                error("LifetimeMode.Bound can't be resumed")
            }

            is LifetimeMode.PausedPersistent -> resumePausedPersistent(act, setup, concept, photocard, scope)
            is LifetimeMode.ContinuousPersistent -> resumeContinuousPersistent(act, setup, concept, photocard, scope)
        }
    }

    fun stop(actId: String): Boolean {
        (activeActs[actId] ?: return false)
            .cancel(StopCancellationException())
        return true
    }

    fun kill(actId: String): Boolean {
        (activeActs[actId] ?: return false)
            .cancel(KillCancellationException())
        return true
    }

    fun isActive(actId: String): Boolean {
        return activeActs.containsKey(actId)
    }

    private fun <T : ActConcept> runBound(
        act: StageAct<T>, setup: ActSetup, concept: T, scope: ActScope
    ): String {
        val id = act.createId(concept)

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val progress = ActProgress(
                firstPerformAt = timeProvider.now() + setup.initialDelay,
                performCount = 0,
                stoppedAt = null,
            )
            var finished = false

            try {
                act.onStart(concept)

                while (true) {
                    val step = boundChoreo.nextStep(setup, progress)

                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    // always 1 since no catch up will ever be done in bound acts
                    act.perform(concept, progress.performCount + 1, 1)
                    progress.performCount += 1

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

    private fun <T : ActConcept> runPausedPersistent(
        act: StageAct<T>, setup: ActSetup, concept: T, scope: ActScope
    ): String {
        val id = act.createId(concept)

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val progress = ActProgress(
                firstPerformAt = timeProvider.now() + setup.initialDelay,
                performCount = 0,
                stoppedAt = null,
            )
            var finished = false

            try {
                act.onStart(concept)

                while (true) {
                    val step = pausedPersistentChoreo.nextStep(setup, progress, false)

                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    // always 1 since no catch up will ever be done in paused persistent acts
                    act.perform(concept, progress.performCount + 1, 1)
                    progress.performCount += 1

                    if (step.isFinished) break
                }

                finished = true
                act.onEndingFairy(concept)
            } catch (e: CancellationException) {
                if (!finished) {
                    withContext(NonCancellable) {
                        val now = timeProvider.now()
                        act.onCancelled(concept, e.toCancellationReason())
                        // for persistent acts, cancelled act that isn't finished
                        // should be persisted to DB (unless they are killed)
                        when (e) {
                            is KillCancellationException -> {
                                deleteAct(scope.ownerId, id)
                            }

                            else -> {
                                // this includes StopCancellationException
                                persistAct(
                                    false, id, scope.ownerId, act.name,
                                    act.createIdentity(concept), progress, now
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
                // when error happens, act is rendered as invalid
                // thus should be removed from DB
                deleteAct(scope.ownerId, id)
            } finally {
                activeActs.remove(id)
            }
        }

        activeActs[id] = job
        return id
    }

    private fun <T : ActConcept> runContinuousPersistent(
        act: StageAct<T>, setup: ActSetup, concept: T, scope: ActScope
    ): String {
        val id = act.createId(concept)

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val progress = ActProgress(
                firstPerformAt = timeProvider.now() + setup.initialDelay,
                performCount = 0,
                stoppedAt = null,
            )
            var finished = false

            try {
                act.onStart(concept)

                while (true) {
                    val step = continuousPersistentChoreo.nextStep(setup, progress, false)

                    if (step.runsTodo > 1) {
                        // there were some missed runs, do it first without delay
                        // runsTodo is only greater than 1 for continuous persistent
                        act.perform(concept, progress.performCount + step.runsTodo - 1, step.runsTodo - 1)
                        progress.performCount += step.runsTodo - 1
                    }

                    // delay for the current perform
                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    // this will always be one since missed runs calculation are above
                    act.perform(concept, progress.performCount + 1, 1)
                    progress.performCount += 1

                    if (step.isFinished) break
                }

                finished = true
                act.onEndingFairy(concept)
            } catch (e: CancellationException) {
                if (!finished) {
                    withContext(NonCancellable) {
                        val now = timeProvider.now()
                        act.onCancelled(concept, e.toCancellationReason())
                        // for persistent acts, cancelled act that isn't finished
                        // should be persisted to DB (unless they are killed)
                        when (e) {
                            is KillCancellationException -> {
                                deleteAct(scope.ownerId, id)
                            }

                            else -> {
                                // this includes StopCancellationException
                                persistAct(
                                    false, id, scope.ownerId, act.name,
                                    act.createIdentity(concept), progress, now
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
                // when error happens, act is rendered as invalid
                // thus should be removed from DB
                deleteAct(scope.ownerId, id)
            } finally {
                activeActs.remove(id)
            }
        }

        activeActs[id] = job
        return id
    }


    private fun <T : ActConcept> resumePausedPersistent(
        act: StageAct<T>, setup: ActSetup, concept: T, photocard: Photocard, scope: ActScope
    ): String {
        val id = photocard.actId
        val progress = photocard.progress

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var finished = false

            try {
                act.onStart(concept)

                while (true) {
                    val step = pausedPersistentChoreo.nextStep(setup, progress, true)

                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    // always 1 since no catch up will ever be done in paused persistent acts
                    act.perform(concept, progress.performCount + 1, 1)
                    progress.performCount += 1

                    if (step.isFinished) break
                }

                finished = true
                act.onEndingFairy(concept)
            } catch (e: CancellationException) {
                if (!finished) {
                    withContext(NonCancellable) {
                        val now = timeProvider.now()
                        act.onCancelled(concept, e.toCancellationReason())
                        // for persistent acts, cancelled act that isn't finished
                        // should be persisted to DB (unless they are killed)
                        when (e) {
                            is KillCancellationException -> {
                                deleteAct(scope.ownerId, id)
                            }

                            else -> {
                                // this includes StopCancellationException
                                persistAct(
                                    true, id, scope.ownerId, act.name,
                                    act.createIdentity(concept), progress, now
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
                // when error happens, act is rendered as invalid
                // thus should be removed from DB
                deleteAct(scope.ownerId, id)
            } finally {
                activeActs.remove(id)
                if (finished) {
                    deleteAct(scope.ownerId, id)
                }
            }
        }

        activeActs[id] = job
        return id
    }

    private fun <T : ActConcept> resumeContinuousPersistent(
        act: StageAct<T>, setup: ActSetup, concept: T, photocard: Photocard, scope: ActScope
    ): String {
        val id = photocard.actId
        val progress = photocard.progress

        val job = scope.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var finished = false

            try {
                act.onStart(concept)

                while (true) {
                    val step = continuousPersistentChoreo.nextStep(setup, progress, false)

                    if (step.runsTodo > 1) {
                        // there were some missed runs, do it first without delay
                        // runsTodo is only greater than 1 for continuous persistent
                        act.perform(concept, progress.performCount + step.runsTodo - 1, step.runsTodo - 1)
                        progress.performCount += step.runsTodo - 1
                    }

                    // delay for the current perform
                    if (step.delay > 0) {
                        delay(step.delay.milliseconds)
                    }

                    // this will always be one since missed runs calculation are above
                    act.perform(concept, progress.performCount + 1, 1)
                    progress.performCount += 1

                    if (step.isFinished) break
                }

                finished = true
                act.onEndingFairy(concept)
            } catch (e: CancellationException) {
                if (!finished) {
                    withContext(NonCancellable) {
                        val now = timeProvider.now()
                        act.onCancelled(concept, e.toCancellationReason())
                        // for persistent acts, cancelled act that isn't finished
                        // should be persisted to DB (unless they are killed)
                        when (e) {
                            is KillCancellationException -> {
                                deleteAct(scope.ownerId, id)
                            }

                            else -> {
                                // this includes StopCancellationException
                                persistAct(
                                    true, id, scope.ownerId, act.name,
                                    act.createIdentity(concept), progress, now
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Fancam.error(e) { "Error on act '${act.name}' for '${scope.ownerId}'." }
                // when error happens, act is rendered as invalid
                // thus should be removed from DB
                deleteAct(scope.ownerId, id)
            } finally {
                activeActs.remove(id)
                if (finished) {
                    deleteAct(scope.ownerId, id)
                }
            }
        }

        activeActs[id] = job
        return id
    }

    private suspend fun persistAct(
        update: Boolean,
        actId: String,
        ownerId: String,
        name: String,
        identity: Map<String, String>,
        progress: ActProgress,
        stoppedAt: Long
    ) {
        val photocard = Photocard(
            actId = actId,
            name = name,
            progress = progress.copy(stoppedAt = stoppedAt),
            data = identity
        )

        if (update) {
            when (ownerId) {
                ServerId -> photocardSubunit.updateServerPhotocard(photocard)
                else -> photocardSubunit.updatePhotocard(ownerId, photocard)
            }
        } else {
            when (ownerId) {
                ServerId -> photocardSubunit.saveServerPhotocard(photocard)
                else -> photocardSubunit.savePhotocard(ownerId, photocard)
            }
        }
    }

    private suspend fun deleteAct(ownerId: String, actId: String) {
        when (ownerId) {
            ServerId -> photocardSubunit.deleteServerPhotocard(actId)
            else -> photocardSubunit.deletePhotocard(ownerId, actId)
        }
    }
}


/* time model for bound and paused persistent
class Schedule

startedAt = 0
firstPerformAt = startedAt + initialDelay = 5

timeToNextExecution = if performCount == 0 time = initialDelay
                    ; now = 1
                    ; time = 5
                    ; delay = 4
                    ; if performCount != 0 time = firstPerformAt + (interval * performCount)
                    ; now = 6
                    ; time = 15 (5 + (10 * 1))
                    ; delay = 9

class Progress
    performCount = 1
    lastActiveAt = 9

now = 30
missedTime = 21 (now - lastActiveAt) 30 - 9

re-sync time model by adding missedTime to firstPerformAt

5 + 21 = 26

timeToNextExecution = time = firstPerformAt + (interval * performCount)
                    ; now = 30
                    ; time = 36 (26 + (10 * 1))
                    ; delay = 6

for continous persistent, no need to re-sync time model

5  15  25  35
  c      30

performCount = 1

missedTime = now - lastActiveAt = 30 - 9 = 21

floor(missedTime / interval) = 2 missed runs

expectedPerformCount = performCount + missedRuns = 3

timeToNextExecution = time = firstPerformAt + (interval * performCount)
                    ; now = 30
                    ; time = 35 (5 + (10 * 3))
                    ; delay = 5

-=-=-==-
every types can use the same method and model
paused persistent: have to adjusted firstPerformAt on resume (newFirstPerformAt = firstPerformAt + missedTime)
continous persistent: same method but no adjustion, but increase performCount by missedRuns


-=-=-==

 */



class ContinuousPersistentChoreography(private val timeProvider: TimeProvider) {
    fun nextStep(setup: ActSetup, progress: ActProgress, resume: Boolean): Step {
        if (resume) {
            val maxMissedRuns = when (setup.lifetimeMode) {
                is LifetimeMode.Bound -> 1
                is LifetimeMode.PausedPersistent -> 1
                is LifetimeMode.ContinuousPersistent -> {
                    setup.lifetimeMode.missedPerformPolicy.maxBatch
                }
            }

            when (setup.performMode) {
                is PerformMode.Once -> {
                    // always 1
                    val newPerformCount = 1
                    val delay = calculateDelay(progress.firstPerformAt, newPerformCount, 0)
                    return Step(delay, 1, isFinished(setup, newPerformCount))
                }

                is PerformMode.Repeat -> {
                    val stoppedAt = requireNotNull(progress.stoppedAt) {
                        "Progress.stoppedAt is null for ContinuousPersistent.Repeat on resume."
                    }
                    val missedTime = timeProvider.now() - stoppedAt
                    val missedRuns = (missedTime / setup.performMode.interval).toInt()
                    val newPerformCount = progress.performCount + missedRuns
                    val delay = calculateDelay(progress.firstPerformAt, newPerformCount, setup.performMode.interval)
                    // the total runs to do is the missedRun (capped with maxMissedRuns)
                    // and added with 1 which is the next new run
                    return Step(delay, min(missedRuns, maxMissedRuns) + 1, isFinished(setup, newPerformCount))
                }

                is PerformMode.Forever -> {
                    val stoppedAt = requireNotNull(progress.stoppedAt) {
                        "Progress.stoppedAt is null for ContinuousPersistent.Repeat on resume."
                    }
                    val missedTime = timeProvider.now() - stoppedAt
                    val missedRuns = (missedTime / setup.performMode.interval).toInt()
                    val newPerformCount = progress.performCount + missedRuns
                    val delay = calculateDelay(progress.firstPerformAt, newPerformCount, setup.performMode.interval)
                    // the total runs to do is the missedRun (capped with maxMissedRuns)
                    // and added with 1 which is the next new run
                    return Step(delay, min(missedRuns, maxMissedRuns) + 1, isFinished(setup, newPerformCount))
                }
            }
        }

        val delay = delayLeft(setup, progress)
        return Step(delay, 1, isFinished(setup, progress.performCount + 1))
    }

    private fun delayLeft(setup: ActSetup, progress: ActProgress): Long {
        return when (setup.performMode) {
            is PerformMode.Once -> {
                calculateDelay(progress.firstPerformAt, progress.performCount, 0)
            }

            is PerformMode.Repeat -> {
                calculateDelay(progress.firstPerformAt, progress.performCount, setup.performMode.interval)
            }

            is PerformMode.Forever -> {
                calculateDelay(progress.firstPerformAt, progress.performCount, setup.performMode.interval)
            }
        }
    }

    private fun calculateDelay(firstPerformAt: Long, performCount: Int, interval: Long): Long {
        val nextPerformAt = firstPerformAt + performCount * interval
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

class PausedPersistentChoreography(private val timeProvider: TimeProvider) {
    fun nextStep(setup: ActSetup, progress: ActProgress, resume: Boolean): Step {
        if (resume) {
            // re-sync time
            val stoppedAt = requireNotNull(progress.stoppedAt) {
                "Progress.stoppedAt is null for PausedPersistent on resume."
            }
            val missedTime = timeProvider.now() - stoppedAt
            progress.firstPerformAt += missedTime
        }

        val delay = delayLeft(setup, progress)
        return Step(delay, 1, isFinished(setup, progress.performCount + 1))
    }

    private fun delayLeft(setup: ActSetup, progress: ActProgress): Long {
        return when (setup.performMode) {
            is PerformMode.Once -> {
                calculateDelay(progress, 0)
            }

            is PerformMode.Repeat -> {
                calculateDelay(progress, setup.performMode.interval)
            }

            is PerformMode.Forever -> {
                calculateDelay(progress, setup.performMode.interval)
            }
        }
    }

    private fun calculateDelay(progress: ActProgress, interval: Long): Long {
        val nextPerformAt = progress.firstPerformAt + progress.performCount * interval
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

class BoundChoreography(private val timeProvider: TimeProvider) {
    fun nextStep(setup: ActSetup, progress: ActProgress): Step {
        val delay = delayLeft(setup, progress)
        return Step(delay, 1, isFinished(setup, progress.performCount + 1))
    }

    private fun delayLeft(setup: ActSetup, progress: ActProgress): Long {
        return when (setup.performMode) {
            is PerformMode.Once -> {
                calculateDelay(progress, 0)
            }

            is PerformMode.Repeat -> {
                calculateDelay(progress, setup.performMode.interval)
            }

            is PerformMode.Forever -> {
                calculateDelay(progress, setup.performMode.interval)
            }
        }
    }

    private fun calculateDelay(progress: ActProgress, interval: Long): Long {
        val nextPerformAt = progress.firstPerformAt + progress.performCount * interval
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
