package user

import FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import user.auth.SessionManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TestSessionManager {
    @Test
    fun `test verify unknown token return false`() {
        val manager = SessionManager()
        assertFalse(manager.verify("asdf"))
    }

    @Test
    fun `test verify unexpired session return true`() {
        val time = FakeTimeProvider(1)
        val manager = SessionManager(time)
        val session = manager.create("pid123", validFor = 1.hours)
        assertTrue(manager.verify(session.token))

        // session max is 1 hr, should still be valid
        time.advance(45.minutes)
        assertTrue(manager.verify(session.token))
    }

    @Test
    fun `test verify expired session return false`() {
        val time = FakeTimeProvider(1)
        val manager = SessionManager(time)
        val session = manager.create("pid123", validFor = 90.minutes)

        // session max is 1.5 hr, this is invalid because it must be refreshed first
        time.advance(2.hours)
        assertFalse(manager.verify(session.token))
    }

    @Test
    fun `test verify session lifetime exceeded the session duration but refreshed in between return true`() {
        val time = FakeTimeProvider(1)
        val manager = SessionManager(time)
        val session = manager.create("pid123", validFor = 1.hours, lifetime = 6.hours)

        time.advance(40.minutes)
        manager.refresh(session.token)
        time.advance(40.minutes)
        assertTrue(manager.verify(session.token))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test verify session refreshed after expired but before exceeding max lifetime return true`() = runTest {
        val time = FakeTimeProvider(1)
        val dispatcher = StandardTestDispatcher()
        val manager = SessionManager(time, dispatcher)
        val session = manager.create("pid123")

        time.advance(2.hours)
        advanceTimeBy(2.hours)
        assertFalse(manager.verify(session.token))
        time.advance(2.hours)
        advanceTimeBy(2.hours)
        assertTrue(manager.refresh(session.token))
        assertTrue(manager.verify(session.token))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test verify session refreshed after expired but after exceeding max lifetime return false`() = runTest {
        val time = FakeTimeProvider(1)
        val dispatcher = StandardTestDispatcher()
        val manager = SessionManager(time, dispatcher)
        val session = manager.create("pid123", validFor = 1.hours, lifetime = 6.hours)

        time.advance(7.hours)
        advanceTimeBy(7.hours)
        assertFalse(manager.verify(session.token))
        assertFalse(manager.refresh(session.token))
        assertFalse(manager.verify(session.token))
    }
}
