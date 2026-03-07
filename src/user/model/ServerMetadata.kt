package user.model

import kotlinx.serialization.Serializable

/**
 * Data owned by a specific player but is an extra and managed at the server level.
 *
 * This may include feature flags, progression milestones, or
 * temporary states that are not part of core player profile data.
 */
@Serializable
data class ServerMetadata(
    val flags: Map<String, Boolean> = emptyMap(),
    val extra: Map<String, String> = emptyMap(),
)
