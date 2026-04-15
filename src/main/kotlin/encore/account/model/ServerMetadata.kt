package encore.account.model

import kotlinx.serialization.Serializable

/**
 * Server-owned information associated with a specific player.
 *
 * Contains non-gameplay data managed at server-level.
 *
 * Typically includes extra data like flags, ban, permissions, or
 * any other metadata for temporary or administrative purposes.
 */
@Serializable
data class ServerMetadata(
    val flags: Map<String, Boolean> = emptyMap(),
    val extra: Map<String, String> = emptyMap(),
)
