package encore.startup.venue

import encore.annotation.VenueKey
import encore.utils.XMLFlattener
import encore.utils.logging.ILogger
import encore.utils.logging.Logger
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * Internal helper responsible for loading and binding configuration.
 *
 * Responsibilities:
 * 1. Parse XML configuration files using [XMLFlattener]
 * 2. Bind flattened keys to configuration data classes via reflection
 * 3. Override values using environment variables when present
 *
 * @param venueFiles configuration files to process
 * @param rootPrefix root XML tag (usually `venue`)
 * @param envProvider component that provides environment variables, intended for unit testing.
 */
class VenuePreparer(
    venueFiles: List<File>,
    private val rootPrefix: String,
    private val envProvider: EnvProvider = SystemEnvProvider(),
    private val logger: ILogger = Logger
) {
    private val flattener = XMLFlattener()
    private val finalKeys = mutableMapOf<String, String>()
    private val usedKeys = mutableSetOf<String>()

    init {
        for (file in venueFiles) {
            processXml(file).forEach { (k, v) ->
                if (k in finalKeys) {
                    logger.warn { "Duplicate configuration key detected: '$k'. Last value wins $v." }
                }
                finalKeys[k] = v
            }
        }
        logger.verbose { "Venue configuration loaded successfully (${finalKeys.size} total entries)." }
    }

    /**
     * Get the instantiated data class via [bind].
     *
     * @param clazz The data class by `ClassName::class`.
     * @param prefix The XML tag prefix before the fields.
     * @throws IllegalStateException May throw exception, see [bind].
     */
    fun <T : Any> get(clazz: KClass<T>, prefix: String): T {
        return bind(finalKeys, "$rootPrefix.$prefix", clazz, usedKeys)
    }

    /**
     * To validates that all listed values on XML are used.
     */
    fun validate() {
        val unused = finalKeys.keys - usedKeys
        if (unused.isEmpty()) return

        logger.warn {
            buildString {
                appendLine("Unused configuration keys detected:")
                unused.sortedBy { it.length }.forEach {
                    appendLine(" - $it")
                }
            }
        }
    }

    private fun processXml(file: File): Map<String, String> {
        val resultVenue = flattener
            .flatten(file.readText(), rootPrefix)
            .toMutableMap()

        logger.verbose { "Loaded ${resultVenue.size} entries from ${file.name}." }
        return resultVenue
    }

    /**
     * Bind a flat key-value [map] into the respective data class definition by [clazz].
     *
     * During the binding process, it will also try checking if an environment variable
     * is defined for the same key. If it is, the environment value will take precedence.
     *
     * @throws IllegalStateException When:
     *  1. [clazz] does not have primary constructor.
     *  2. [clazz] does not annotate all non-data value with [VenueKey].
     *  3. Config key is missing from [map], data class does not have a default value, and
     *     environment variable wasn't present.
     */
    private fun <T : Any> bind(
        map: Map<String, String>,
        prefix: String,
        clazz: KClass<T>,
        usedKeys: MutableSet<String>
    ): T {
        val constructor = clazz.primaryConstructor
            ?: error("Class ${clazz.simpleName} must have a primary constructor")

        val args = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            val type = param.type.classifier as KClass<*>

            // nested config
            if (type.isData) {
                args[param] = bind(map, prefix, type, usedKeys)
                continue
            }

            val keyAnn = param.findAnnotation<VenueKey>()
                ?: error("Field is value but missing @VenueKey on ${clazz.simpleName}.${param.name}")

            val key = "$prefix.${keyAnn.path}"
            val value = checkEnv(key) ?: map[key]

            if (value == null) {
                if (!param.isOptional) {
                    throw IllegalStateException(
                        "Config value: $key is missing from XML and ENV."
                    )
                }
                continue
            }

            usedKeys += key
            args[param] = convert(value, type)
        }

        return constructor.callBy(args)
    }

    /**
     * Check whether environment variable is defined for the [key].
     *
     * @return The defined value. `null` if it's not defined.
     */
    private fun checkEnv(key: String): String? {
        return envProvider.get(pathkeyToEnv(key))
    }

    private fun pathkeyToEnv(path: String): String {
        val trimmed = path.removePrefix("$rootPrefix.")
        return "ENCORE_" + trimmed.replace(".", "_").uppercase()
    }

    /**
     * @throws IllegalStateException if the target type is not supported.
     */
    private fun convert(value: String, type: KClass<*>): Any {
        return when (type) {
            String::class -> value
            Int::class -> value.toInt()
            Boolean::class -> value.toBoolean()
            Long::class -> value.toLong()
            Double::class -> value.toDouble()
            Float::class -> value.toFloat()
            else -> error("Unsupported config type: ${type.simpleName}")
        }
    }
}
