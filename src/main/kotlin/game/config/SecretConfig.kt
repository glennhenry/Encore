package game.config

/**
 * Definition of custom config for secret.
 *
 * These entries should be listed in `venue.secret.xml`.
 * The file is not tracked by Git.
 *
 * All field should be immutable for discipline.
 */
data class SecretConfig(
    val dummy: Int = 0
)
