package encore.startup.venue

import encore.annotation.VenueKey
import encore.utils.XMLFlattener
import encore.utils.logging.Logger
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * Prepares [Venue] by:
 * 1. Process XML files with [XMLFlattener].
 * 2. Bind XML output to data classes definition with reflection.
 *
 * Initialize class with list of [venueFiles] and use [get] providing
 * the config data class to be initialized.
 *
 * @param venueFiles Venue of files to process (only `venue.xml` and `venue.secret.xml`)
 * @param rootPrefix The root XML tag, usually 'venue'.
 */
class VenuePreparer(venueFiles: List<File>, private val rootPrefix: String) {
    private val flattener = XMLFlattener()
    private val finalKeys = mutableMapOf<String, String>()
    private val usedKeys = mutableSetOf<String>()

    init {
        for (file in venueFiles) {
            processXml(file).forEach { (k, v) ->
                if (k in finalKeys) {
                    Logger.warn { "Venue.prepare: duplicate key '$k' found." }
                }
                finalKeys[k] = v
            }
        }
        Logger.verbose { "Venue.prepare: configuration loaded successfully (${finalKeys.size} total entries)." }
    }

    /**
     * Get the instantiated data class via [bind].
     *
     * @param clazz The data class by `ClassName::class`.
     * @param prefix The XML tag prefix before the fields.
     */
    fun <T : Any> get(clazz: KClass<T>, prefix: String): T {
        return bind(finalKeys, "$rootPrefix.$prefix", clazz, usedKeys)
    }

    /**
     * To validates that all listed values on XML is used.
     */
    fun validate() {
        val unused = finalKeys.keys - usedKeys
        if (unused.isEmpty()) return

        Logger.warn {
            buildString {
                appendLine("Unused configuration keys detected:")
                unused.sorted().forEach {
                    appendLine(" - $it")
                }
            }
        }
    }

    private fun processXml(file: File): Map<String, String> {
        val resultVenue = flattener
            .flatten(file.readText(), rootPrefix)
            .toMutableMap()

        Logger.verbose { "Venue.prepare: loaded ${resultVenue.size} entries from ${file.name}." }
        return resultVenue
    }

    /**
     * Bind a flat key-value [map] with specific [prefix] (usually based on XML tag)
     * into the respective data class definition by [clazz].
     *
     * @throws IllegalStateException When:
     *                               1. [clazz] does not have primary constructor.
     *                               2. [clazz] does not annotate all value with [VenueKey].
     *                               3. Config key is missing from [map] and data class does not have default.
     */
    private fun <T : Any> bind(map: Map<String, String>, prefix: String, clazz: KClass<T>, usedKeys: MutableSet<String>): T {
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
            val raw = map[key]

            if (raw == null) {
                if (!param.isOptional) {
                    throw IllegalStateException("Missing config value: $key")
                }
                continue
            }

            usedKeys += key
            args[param] = convert(raw, type)
        }

        return constructor.callBy(args)
    }

    /**
     * @throws IllegalStateException When getting unsupported [value] type.
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
