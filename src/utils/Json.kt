@file:Suppress("UNCHECKED_CAST")

package utils

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import utils.logging.Logger
import kotlin.collections.map
import kotlin.collections.mapValues
import kotlin.reflect.KClass

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
fun Any?.toJsonValue(prioritizeToString: Boolean = false): JsonElement = when (this) {
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
        if (prioritizeToString) {
            JsonPrimitive(this.toString())
        } else {
            val kClass = this::class
            val serializer = runCatching {
                JSON.json.serializersModule.getContextual(kClass) ?: kClass.serializer()
            }.getOrNull()

            when {
                serializer != null -> {
                    JSON.json.encodeToJsonElement(serializer as SerializationStrategy<Any>, this)
                }
                else -> {
                    Logger.warn { "Serializer missing for $kClass, used 'toString()' fallback." }
                    JsonPrimitive(this.toString())
                }
            }
        }
    }
}
