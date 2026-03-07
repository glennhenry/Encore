package user.auth

import user.model.UserSession
import core.data.AdminData
import kotlinx.coroutines.CoroutineDispatcher
import utils.functions.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import utils.functions.SystemTime
import utils.functions.TimeProvider
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Manages authentication sessions for online users.
 *
 * A user's session is identified by `userId` and verification is done with an UUID token.
 *
 * By default, individual sessions have a **1-hour timeout**, but can be refreshed
 * by the player to extend the total session lifetime up to the **6-hour maximum**.
 */
class SessionManager(
    private val time: TimeProvider = SystemTime,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val sessions = ConcurrentHashMap<String, UserSession>()
    private val cleanUpInterval = 5 * 60 * 1000L // 5 minutes
    private val cleanupJob = Job()
    private val scope = CoroutineScope(dispatcher + cleanupJob)

    init {
        scope.launch {
            while (isActive) {
                cleanupExpiredSessions()
                delay(cleanUpInterval)
            }
        }
    }

    /**
     * Create session for the [userId] with:
     * - duration of [validFor], default 1 hour
     * - lifetime of [lifetime], default 6 hours.
     */
    fun create(userId: String, validFor: Duration = 1.hours, lifetime: Duration = 6.hours): UserSession {
        val now = time.now()

        val token = if (userId == AdminData.PLAYER_ID) {
            AdminData.TOKEN
        } else {
            UUID.new()
        }

        val session = UserSession(
            userId = userId,
            token = token,
            issuedAt = now,
            expiresAt = now + validFor.inWholeMilliseconds,
            singleSessionDuration = validFor,
            lifetime = lifetime.inWholeMilliseconds
        )

        sessions[token] = session
        return session
    }

    /**
     * Verify user's session validity from the given [token].
     *
     * This checks whether the token is valid
     * (i.e., the token was issued before and doesn't expire yet).
     *
     * @return `true` if session is valid
     */
    fun verify(token: String): Boolean {
        val session = sessions[token] ?: return false
        val now = time.now()

        return now < session.expiresAt
    }

    /**
     * Refresh player's session from the given [token].
     *
     * First, it checks whether the token is valid
     * (i.e., the token was issued before and doesn't exceed the maximum lifetime).
     *
     * @return `true` if session was successfully refreshed.
     */
    fun refresh(token: String): Boolean {
        val session = sessions[token] ?: return false
        val now = time.now()

        val usedLifetime = now - session.issuedAt
        if (usedLifetime > session.lifetime) {
            sessions.remove(token)
            return false
        }

        session.expiresAt = now + session.singleSessionDuration.inWholeMilliseconds
        return true
    }

    /**
     * Get the `userId` associated with this [token].
     *
     * @return `null` if the token is invalid.
     */
    fun getUserId(token: String): String? {
        return sessions[token]?.takeIf { time.now() < it.expiresAt }?.userId
    }

    /**
     * Cleanup expired sessions, which has exceeded the maximum lifetime.
     */
    private fun cleanupExpiredSessions() {
        val now = time.now()
        val expiredKeys = sessions.filterValues { now - it.issuedAt > it.lifetime }.keys
        expiredKeys.forEach { sessions.remove(it) }
    }

    fun shutdown() {
        cleanupJob.cancel()
        runBlocking { cleanupJob.join() }
        sessions.clear()
    }
}
