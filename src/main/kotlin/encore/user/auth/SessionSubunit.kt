package encore.user.auth

import encore.server.ServerContainer
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.user.AdminData
import encore.user.model.UserSession
import encore.utils.Ids
import encore.utils.SystemTime
import encore.utils.TimeProvider
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Server-scoped subunit responsible to manages sessions of online users.
 *
 * A [UserSession] is identified by `userId` and verification is done with UUID.
 *
 * - Use [create] to generate a session.
 * - Use [verify] passing a token to verify the session's validity.
 * - Use [refresh] to extend the session's duration.
 *
 * @param parentScope The parent coroutine scope that holds the lifecycle (typically [ServerContainer]).
 * @param time [TimeProvider] implementation, default is [SystemTime].
 */
class SessionSubunit(
    private val parentScope: CoroutineScope,
    private val time: TimeProvider = SystemTime,
) : Subunit<ServerScope> {
    // token to UserSession
    private val sessions = ConcurrentHashMap<String, UserSession>()
    private val cleanUpInterval = 5.minutes
    private val cleanupJob: Job

    init {
        cleanupJob = parentScope.launch {
            while (isActive) {
                delay(cleanUpInterval)
                cleanupExpiredSessions()
            }
        }
    }

    /**
     * Create a session for the [userId] with:
     * - base duration of [validFor], default 1 hour.
     * - lifetime of [lifetime], default 6 hours.
     */
    fun create(userId: String, validFor: Duration = 1.hours, lifetime: Duration = 6.hours): UserSession {
        val now = time.now()

        val token = if (userId == AdminData.PLAYER_ID) {
            AdminData.TOKEN
        } else {
            Ids.uuid()
        }

        val session = UserSession(
            userId = userId,
            token = token,
            baseDuration = validFor,
            issuedAt = now,
            expiresAt = now + validFor.inWholeMilliseconds,
            lifetime = lifetime.inWholeMilliseconds
        )

        sessions[token] = session
        return session
    }

    /**
     * Verify the validity of session associated with the [token].
     *
     * This checks whether the token is valid:
     * - the token was issued before
     * - the token doesn't expire yet
     *
     * @return `true` if session is valid, `false` otherwise.
     */
    fun verify(token: String): Boolean {
        val session = sessions[token] ?: return false
        val now = time.now()

        return now < session.expiresAt
    }

    /**
     * Refresh a session associated with the [token].
     *
     * Before refreshing, the token is checked:
     * - if it was issued before
     * - doesn't exceed the maximum lifetime
     *
     * @return `true` if session was successfully refreshed, `false` otherwise.
     */
    fun refresh(token: String): Boolean {
        val session = sessions[token] ?: return false
        val now = time.now()

        val usedLifetime = now - session.issuedAt
        if (usedLifetime > session.lifetime) {
            sessions.remove(token)
            return false
        }

        session.expiresAt = now + session.baseDuration.inWholeMilliseconds
        return true
    }

    /**
     * Get the `userId` associated with this [token].
     *
     * The token is valid when:
     * - the token was issued before
     * - the token doesn't expire yet
     *
     * @return `null` if the token is invalid.
     */
    fun getUserId(token: String): String? {
        return sessions[token]?.takeIf { time.now() < it.expiresAt }?.userId
    }

    /**
     * Cleanup expired sessions which has exceeded the maximum lifetime
     * and can't be refreshed anymore.
     */
    private fun cleanupExpiredSessions() {
        val now = time.now()
        sessions.entries.removeIf { (_, session) ->
            now - session.issuedAt > session.lifetime
        }
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching {
            cleanupJob.cancelAndJoin()
            sessions.clear()
            return Result.success(Unit)
        }
    }
}
