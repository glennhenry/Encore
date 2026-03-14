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
 * Configuration values are primarily defined in `venue.xml`. Sensitive values
 * can be placed in `venue.secret.xml`. Environment variables may override
 * any value defined in the XML configuration.
 *
 * The configuration is loaded during [prepare]. XML values are flattened and
 * bound to strongly typed configuration classes such as:
 * - [EncoreConfig] — framework configuration
 * - [CustomConfig] — user-defined application configuration
 * - [SecretConfig] — sensitive values
 *
 * Environment variables follow the same key path as [VenueKey] definitions.
 * For example:
 * - `database.prod.name` → `ENCORE_DATABASE_PROD_NAME`
 * - `color._enabled` → `ENCORE_COLOR__ENABLED`
 *
 * Where `ENCORE_` correspond to set prefix [ENCORE_ENV_PREFIX].
 *
 * Values set from ENV can also be accessed directly using [get].
 *
 * The loader validates configuration during startup and fails fast when:
 * - required values are missing
 * - types cannot be converted
 * - configuration structure is invalid
 *
 * Configuration properties must use the [VenueKey] annotation to define the
 * corresponding XML path.
 *
 * @property encore Framework configuration used internally by Encore.
 * @property custom Application-specific configuration defined by the game.
 * @property secret Sensitive configuration loaded from `venue.secret.xml`.
 */
object Venue {
    private var done: Boolean = false
    lateinit var encore: EncoreConfig
    lateinit var custom: CustomConfig
    lateinit var secret: SecretConfig

    /**
     * Loads and validates configuration from `venue.xml` and `venue.secret.xml`.
     *
     * This method must be called before accessing any configuration values.
     *
     * @throws IllegalStateException if:
     *  - `venue.xml` is missing
     *  - configuration binding fails
     *  - required configuration values are missing
     */
    fun prepare() {
        if (done) {
            Logger.warn { "Venue.prepare() called after initialization. Ignoring." }
            return
        }
        Logger.info { "Loading venue configuration" }

        val venueFile = File("venue.xml")
        val venueSecretFile = File("venue.secret.xml")

        if (!venueFile.exists()) {
            throw IllegalStateException(
                "Expected 'venue.xml' in the root directory, but the file is missing."
            )
        }

        val preparer = VenuePreparer(buildList {
            add(venueFile)
            if (venueSecretFile.exists()) add(venueSecretFile)
        })

        encore = preparer.get(EncoreConfig::class, VenueCategory.ENCORE, ENCORE_ENV_PREFIX)
        custom = preparer.get(CustomConfig::class, VenueCategory.CUSTOM, ENCORE_ENV_PREFIX)
        secret = preparer.get(SecretConfig::class, VenueCategory.SECRET, ENCORE_ENV_PREFIX)
        preparer.validate()

        Logger.info { "Venue preparation finished." }
        done = true
    }

    /**
     * Returns the raw value of an environment variable.
     *
     * This method does not read values from `venue.xml`. It only accesses
     * variables defined in the system environment.
     *
     * @param name Environment variable name.
     * @return the value, or `null` if the variable is not defined.
     */
    fun get(name: String): String? {
        return System.getenv(name)
    }
}

/**
 * Venue category that groups the domain of config.
 * - `encore` for [EncoreConfig]
 * - `custom` for [CustomConfig]
 * - `secret` for [SecretConfig]
 */
object VenueCategory {
    const val ENCORE = "encore"
    const val CUSTOM = "custom"
    const val SECRET = "secret"
}

/**
 * Prefix used to define ENV variable to avoid conflict.
 *
 * This prefix is added before the config variable name, for instance:
 * - `ENCORE_SERVER_HOST`
 * - `ENCORE_DATABASE_NAME`
 */
const val ENCORE_ENV_PREFIX = "ENCORE"

/**
 * The XML root tag in `venue.xml`.
 */
const val VENUE_ROOT_TAG = "venue"
