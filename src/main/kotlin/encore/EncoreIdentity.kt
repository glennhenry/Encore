@file:Suppress("ConstPropertyName", "unused")

package encore

import game.GameIdentity

/**
 * Defines identity of the Encore framework.
 *
 * Provides a mainly cosmetic, code-level details
 * such as product name, version, slogan, and logo.
 *
 * This information is optional and used only for presentation,
 * for example in server startup banners or log messages.
 *
 * Though, the version can be modified when the framework receives update,
 * ensuring noticeable difference on the server implementation.
 */
object EncoreIdentity {
    const val Title = "Encore"
    const val Version = "0.0.0"
    const val VersionDate = "2026.04.10"
    const val Codename = "Training"
    const val Slogan = "Bring it back live."
    const val Logo = """
  _____ _   _  ____ ___  ____  _____ 
 | ____| \ | |/ ___/ _ \|  _ \| ____|
 |  _| |  \| | |  | | | | |_) |  _|  
 | |___| |\  | |__| |_| |  _ <| |___ 
 |_____|_| \_|\____\___/|_| \_\_____|
"""

    fun banner(gameTitle: String, gameVersion: String, gameDescription: String): String {
        return bannerText(gameTitle, gameVersion, gameDescription)
    }

    fun banner(gameIdentity: GameIdentity): String {
        return bannerText(gameIdentity.Title, gameIdentity.Version, gameIdentity.Description)
    }

    private fun bannerText(gameTitle: String, gameVersion: String, gameDescription: String): String {
        val t = gameTitle.ifBlank { "Unnamed" }
        val v = gameVersion.ifBlank { "N/A" }

        return buildString {
            appendLine("_______________________________________________")
            appendLine(Logo.trim('\n'))
            appendLine("-----------------------------------------------")
            appendLine("Era    : $Version at $VersionDate ($Codename)")
            appendLine("Stage  : $t ($v)")
            appendLine("         $gameDescription")
            if (gameDescription.isNotBlank()) {
                appendLine()
            }
            appendLine("Made possible by $Title. $Slogan")
            appendLine("_______________________________________________")
        }
    }
}
