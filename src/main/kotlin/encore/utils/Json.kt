@file:Suppress("UNCHECKED_CAST")

package encore.utils

import encore.utils.logging.Fancam
import encore.utils.logging.LOG_INDENT_PREFIX
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Preset JSON serialization and deserialization.
 */
@Suppress("unused")
object JSON {
    lateinit var json: Json

    fun initialize(json: Json) {
        this.json = json
    }

    inline fun <reified T> encode(value: T): String {
        return json.encodeToString<T>(value)
    }

    inline fun <reified T> encode(serializer: SerializationStrategy<T>, value: T): String {
        return json.encodeToString(serializer, value)
    }

    inline fun <reified T> decode(value: String): T {
        return json.decodeFromString<T>(value)
    }

    inline fun <reified T> decode(deserializer: DeserializationStrategy<T>, value: String): T {
        return json.decodeFromString(deserializer, value)
    }
}

@Suppress("unused")
fun parseJsonToMap(json: String): Map<String, Any?> {
    return try {
        val parsed = JSON.decode<JsonObject>(json)
        parsed.mapValues { (_, v) -> parseJsonElement(v) }
    } catch (_: Exception) {
        emptyMap()
    }
}

fun parseJsonElement(el: JsonElement): Any = when (el) {
    is JsonPrimitive -> {
        when {
            el.isString -> el.content
            el.booleanOrNull != null -> el.boolean
            el.intOrNull != null -> el.int
            el.longOrNull != null -> el.long
            el.doubleOrNull != null -> el.double
            else -> el.content
        }
    }

    is JsonObject -> el.mapValues { parseJsonElement(it.value) }
    is JsonArray -> el.map { parseJsonElement(it) }
}

fun Map<String, *>?.toJsonElement(): JsonObject = buildJsonObject {
    this@toJsonElement?.forEach { (key, value) ->
        put(key, value.toJsonValue())
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
fun Any?.toJsonValue(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> (this as? Map<String, *>)?.toJsonElement()
        ?: error("Map keys must be strings: $this")

    is Iterable<*> -> buildJsonArray { this@toJsonValue.forEach { add(it.toJsonValue()) } }
    is Pair<*, *> -> buildJsonObject {
        put("first", first.toJsonValue())
        put("second", second.toJsonValue())
    }

    is Triple<*, *, *> -> buildJsonObject {
        put("first", first.toJsonValue())
        put("second", second.toJsonValue())
        put("triple", third.toJsonValue())
    }

    else -> {
        val kClass = this::class
        val serializer = runCatching {
            JSON.json.serializersModule.getContextual(kClass) ?: kClass.serializer()
        }.getOrNull()

        when {
            serializer != null -> {
                JSON.json.encodeToJsonElement(serializer as SerializationStrategy<Any>, this)
            }

            else -> {
                Fancam.warn {
                    buildString {
                        appendLine("Serializer missing for ${kClass.simpleName} (${kClass.qualifiedName}). Falling back to 'toString()'.")
                        appendLine("$LOG_INDENT_PREFIX This may be caused by:")
                        appendLine("$LOG_INDENT_PREFIX     - When TrackEvent.data contains a typed object without @Serializable, resulting in kotlinx.serialization not knowing how to serialize it.")
                        appendLine("$LOG_INDENT_PREFIX       You must annotate with @Serializable if you want this field to serialize nestedly in JSON.")
                    }
                }
                JsonPrimitive(this.toString())
            }
        }
    }
}
