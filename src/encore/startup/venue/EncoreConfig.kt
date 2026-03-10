package encore.startup.venue

import encore.annotation.VenueKey

/**
 * Definition of config required by the framework.
 * Define them in `venue.xml` or here directly (for default value).
 *
 * @property devMode
 * @property server
 * @property adminEnabled
 * @property database
 * @property logger
 */
data class EncoreConfig(
    /**
     * Whether to enable development mode, which will enable feature
     * like hot reload and other internal Ktor engine machinery.
     *
     * This will override JVM property `io.ktor.development` and should
     * be set to `false` on deployment.
     */
    @VenueKey("devMode")
    val devMode: Boolean = true,

    val server: EncoreServerConfig,

    /**
     * Whether to enable admin account, which is used for quick and privileged
     * testing in the game, usually can be used by anyone.
     * Credentials are set in code.
     */
    @VenueKey("admin._enabled")
    val adminEnabled: Boolean = true,

    val database: EncoreDatabaseConfig,
    val logger: EncoreLoggerConfig
)

/**
 * @property host
 * @property port
 * @property socketPort
 */
data class EncoreServerConfig(
    /**
     * Host URL where this server is serving from.
     */
    @VenueKey("server.host")
    val host: String,

    /**
     * Server port for file and API server.
     */
    @VenueKey("server.port")
    val port: Int,

    /**
     * Server port for socket connection.
     */
    @VenueKey("server.socketPort")
    val socketPort: Int
)

/**
 * Database configuration which is tailored to Mongo
 */
data class EncoreDatabaseConfig(
    /**
     * Database name used in non-tests environment (dev/production)
     */
    @VenueKey("database.mongo.prod.dbname")
    val dbNameProd: String,

    /**
     * Database URL used in non-tests environment (dev/production)
     */
    @VenueKey("database.mongo.prod.dburl")
    val dbUrlProd: String,

    /**
     * Database name used in tests environment
     */
    @VenueKey("database.mongo.test.dbname")
    val dbNameTest: String,

    /**
     * Database URL used in tests environment
     */
    @VenueKey("database.mongo.test.dburl")
    val dbUrlTest: String,
)

data class EncoreLoggerConfig(
    /**
     * Filter the minimum level of log messages.
     *
     * Levels: `0: ALL` `1: TRACE` `2: DEBUG` `3: INFO`
     *         `4: WARN` `5: ERROR` `6: NOTHING`
     *
     * e.g., level INFO means only seeing log message with INFO, WARN, ERROR
     */
    @VenueKey("logger.level")
    val level: String = "TRACE",

    /**
     * Whether to color log messages or output as plain text.
     */
    @VenueKey("logger.color._enabled")
    val colorEnabled: Boolean = true,

    /**
     * Whether to color the entire message or just the log level label.
     *
     * Recommend set to `false` to reduce light
     */
    @VenueKey("logger.color.colorEntireMessage")
    val colorEntireMessage: Boolean = false,

    /**
     * Whether to color the background instead of the fonts
     *
     * Recommend set to `true` in light mode
     */
    @VenueKey("logger.color.useBackgroundColor")
    val useBackgroundColor: Boolean = true,

    /**
     * Specify the empty space allocation to show a hyperlinked filename
     */
    @VenueKey("logger.formatting.fileNamePadding")
    val fileNamePadding: Int = 25,

    /**
     * Maximum message length for a single line (by `\n`)
     * if `logFull=false` in the log event.
     */
    @VenueKey("logger.formatting.maximumLineLength")
    val maximumLineLength: Int = 500,

    /**
     * Maximum file size in MB (for all files)
     */
    @VenueKey("logger.formatting.maxFileSize")
    val maxFileSize: Int = 5,

    /**
     * Maximum file rotation (for all files)
     */
    @VenueKey("logger.formatting.maxFileRotation")
    val maxFileRotation: Int = 5,

    /**
     * Timestamp format in Java SimpleDateFormat
     */
    @VenueKey("logger.formatting.timestampFormat")
    val timestampFormat: String = "HH:mm:ss.SSS",
)
