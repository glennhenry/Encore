package user.model

import core.data.AdminData
import io.ktor.util.date.getTimeMillis
import kotlinx.serialization.Serializable

/**
 * Server-level profile of a user.
 */
@Serializable
data class UserProfile(
    val playerId: String,
    val email: String = "",
    val displayName: String,
    val createdAt: Long,
    val lastLogin: Long,
) {
    companion object {
        fun admin(): UserProfile {
            val now = getTimeMillis()
            return UserProfile(
                playerId = AdminData.PLAYER_ID,
                email = AdminData.EMAIL,
                displayName = AdminData.DISPLAY_NAME,
                createdAt = now,
                lastLogin = now
            )
        }

        fun default(playerId: String, displayName: String): UserProfile {
            val now = getTimeMillis()
            return UserProfile(
                playerId = playerId,
                email = "",
                displayName = displayName,
                createdAt = now,
                lastLogin = now
            )
        }
    }
}
