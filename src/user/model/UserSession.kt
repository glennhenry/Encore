package user.model

import data.collection.PlayerAccount
import kotlin.time.Duration

/**
 * Representation of a user's authentication session.
 *
 * @property userId User's ID to which this session belongs to,
 *                  can be used to link to anything (e.g., [PlayerAccount]).
 * @property token A unique prove for authentication.
 * @property issuedAt Epoch millis when this session was created.
 * @property expiresAt Epoch millis when this session is no longer valid.
 * @property lifetime Epoch millis of token's total validity if refreshed regularly.
 */
data class UserSession(
    val userId: String,
    val token: String,
    val issuedAt: Long,
    val singleSessionDuration: Duration,
    var expiresAt: Long,
    var lifetime: Long,
)
