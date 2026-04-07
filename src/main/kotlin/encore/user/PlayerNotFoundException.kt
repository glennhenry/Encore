package encore.user

import encore.datastore.collection.PlayerId

/**
 * Thrown when a MongoDB query expected a document from [playerId] but found none.
 */
class PlayerNotFoundException(playerId: PlayerId) : RuntimeException("Player not found: $playerId")
