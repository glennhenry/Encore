package encore.backstage.command

import encore.acts.ActiveActInfo
import kotlinx.serialization.Serializable

/**
 * Monitor data for backstage client.
 */
@Serializable
data class MonitorData(
    val numPlayers: Int,
    val acts: List<ActiveActInfo>
)
