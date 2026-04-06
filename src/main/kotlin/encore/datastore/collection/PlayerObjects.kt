package encore.datastore.collection

import encore.user.AdminData

/**
 * Database-level representation of a player's game data.
 */
data class PlayerObjects(
    val playerId: String,
) {
    companion object {
        fun admin(): PlayerObjects {
            return PlayerObjects(
                playerId = AdminData.PLAYER_ID,
            )
        }

        fun newGame(playerId: String): PlayerObjects {
            return PlayerObjects(
                playerId = playerId,
            )
        }
    }
}
