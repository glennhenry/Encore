package core.model

data class GameData(
    val example: String = "REPLACE"
) {
    companion object {
        fun admin(): GameData {
            return GameData(
                example = "example"
            )
        }

        fun newGame(): GameData {
            return GameData(
                example = "new"
            )
        }
    }
}
