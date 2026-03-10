package encore.startup.venue

import encore.annotation.VenueKey
import encore.startup.venue.Venue.custom
import encore.startup.venue.Venue.encore
import encore.startup.venue.Venue.secret
import encore.utils.logging.Logger
import game.config.CustomConfig
import game.config.SecretConfig
import java.io.File

/**
 * Venue provides access to application configuration and environment variables.
 *
 * Configuration values are defined in `venue.xml` and optionally
 * `venue.secret.xml` for sensitive values. Both files are loaded during
 * application startup.
 *
 * During initialization, the configuration is parsed and mapped to the
 * expected data classes (e.g., [EncoreConfig], [CustomConfig], and
 * [SecretConfig]). The loader validates the structure and will fail fast
 * if any required value is missing (and data classes don't specify default)
 * or does not match the expected type.
 *
 * Fields should use the [VenueKey] annotation to explicitly define the
 * corresponding XML path. This allows configuration classes to use a
 * structure that differs from the XML layout while still being mapped
 * correctly during parsing.
 *
 * @property encore Framework configuration used internally by Encore.
 * @property custom Application-specific configuration defined by the game.
 * @property secret Sensitive configuration values loaded from `venue.secret.xml`.
 */
object Venue {
    private var done: Boolean = false
    lateinit var encore: EncoreConfig
    lateinit var custom: CustomConfig
    lateinit var secret: SecretConfig

    /**
     * Prepare venue.
     *
     * @throws IllegalStateException when `venue.xml` file is missing.
     */
    fun prepare() {
        if (done) {
            Logger.warn { "Venue.prepare() called after initialization. Ignoring." }
            return
        }
        Logger.verbose { "Venue.prepare: loading venue configuration." }

        val venueFile = File("venue.xml")
        val venueSecretFile = File("venue.secret.xml")

        if (!venueFile.exists()) {
            throw IllegalStateException(
                "Venue.prepare: expected 'venue.xml' in the root directory, but the file is missing."
            )
        }

        val preparer = VenuePreparer(mutableListOf(venueFile).also {
            if (venueSecretFile.exists()) {
                it.add(venueSecretFile)
            }
        }, "venue")

        encore = preparer.get(EncoreConfig::class, "encore")
        custom = preparer.get(CustomConfig::class, "custom")
        secret = preparer.get(SecretConfig::class, "secret")
        preparer.validate()

        Logger.verbose { "Venue.prepare: venue preparation finished." }
        done = true
    }
}
