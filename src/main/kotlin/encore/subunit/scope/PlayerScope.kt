package encore.subunit.scope

import encore.datastore.collection.PlayerId

/**
 * A player-scoped context.
 *
 * Subunits using this scope operate on a single player's domain.
 * They may use [playerId] to retrieve and persist player-specific data.
 *
 * @property playerId Player ID associated with this scope.
 */
data class PlayerScope(val playerId: PlayerId) : SubunitScope
