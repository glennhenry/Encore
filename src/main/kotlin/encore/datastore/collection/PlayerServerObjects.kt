package encore.datastore.collection

import encore.account.model.PlayerMetadata
import encore.acts.photocard.model.Photocard
import game.AdminData

/**
 * Represents server-managed player data.
 *
 * This model contains player-owned, non-gameplay related data that is
 * managed by the server. It may include administrative metadata
 * (e.g., bans, flags, temporary states) or per-player tracking data
 * that is needed by the server but isn't classified as game data.
 *
 * @property playerId Unique identifier of the player.
 * @property metadata Miscellaneous information about the player.
 * @property photocards Contains player's photocards.
 */
data class PlayerServerObjects(
    val playerId: PlayerId,
    val metadata: PlayerMetadata = PlayerMetadata(),
    val photocards: List<Photocard> = emptyList()
) {
    /**
     * Template to create player server objects.
     *
     * Creation method is written here and updated accordingly
     * to avoid frequent modification in the framework code.
     */
    companion object {
        fun admin(): PlayerServerObjects {
            return PlayerServerObjects(
                playerId = AdminData.PLAYER_ID,
                metadata = PlayerMetadata(),
                photocards = emptyList(),
            )
        }

        fun newGame(playerId: PlayerId): PlayerServerObjects {
            return PlayerServerObjects(
                playerId = playerId,
                metadata = PlayerMetadata(),
                photocards = emptyList(),
            )
        }
    }
}
