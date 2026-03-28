package encore.db.collection

import encore.user.AdminData

/**
 * Database-level representation of a player's game data.
 */
data class PlayerData(
    val playerId: String,
) {
    companion object {
        fun admin(): PlayerData {
            return PlayerData(
                playerId = AdminData.PLAYER_ID,
            )
        }

        fun newGame(playerId: String): PlayerData {
            return PlayerData(
                playerId = playerId,
            )
        }
    }
}
