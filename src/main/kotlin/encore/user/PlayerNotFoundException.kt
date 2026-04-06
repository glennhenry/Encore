package encore.user

/**
 * Thrown when a MongoDB query expected a document from [playerId] but found none.
 */
class PlayerNotFoundException(playerId: String) : RuntimeException("Player not found: $playerId")
