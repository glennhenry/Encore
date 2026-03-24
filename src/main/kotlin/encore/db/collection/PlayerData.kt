package encore.db.collection

import encore.core.data.AdminData
import encore.core.model.GameData

/**
 * Database-level representation of a player's game data.
 */
data class PlayerData(
    val playerId: String,
    val data: GameData
) {
    companion object {
        fun admin(): PlayerData {
            return PlayerData(
                playerId = AdminData.PLAYER_ID,
                data = GameData.admin()
            )
        }

        fun newGame(playerId: String): PlayerData {
            return PlayerData(
                playerId = playerId,
                data = GameData.newGame()
            )
        }
    }
}
