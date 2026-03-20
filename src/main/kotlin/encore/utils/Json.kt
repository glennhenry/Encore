@file:Suppress("UNCHECKED_CAST")

package encore.utils

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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
        put(key, value.toJsonElement())
    }
}

/**
 * Convert `Any?` type to [JsonElement].
 *
 * @param useReflection Whether to use reflection to convert the object into
 *                      a full JSON graph. Without this, any unserializable object
 *                      (i.e., without `@Serializable` annotation) will fallback
 *                      to `toString()`.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
fun Any?.toJsonElement(useReflection: Boolean = false, visited: MutableSet<Int> = mutableSetOf()): JsonElement {
    val id = System.identityHashCode(this)
    if (this != null && !visited.add(id)) {
        return JsonPrimitive("<cycle>")
    }

    return when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> buildJsonObject {
            this@toJsonElement.forEach { (k, v) ->
                put(k.toString(), v.toJsonElement(useReflection, visited))
            }
        }

        is Iterable<*> -> buildJsonArray {
            this@toJsonElement.forEach {
                add(
                    it.toJsonElement(
                        useReflection,
                        visited
                    )
                )
            }
        }

        is Pair<*, *> -> buildJsonObject {
            put("first", first.toJsonElement(useReflection, visited))
            put("second", second.toJsonElement(useReflection, visited))
        }

        is Triple<*, *, *> -> buildJsonObject {
            put("first", first.toJsonElement(useReflection, visited))
            put("second", second.toJsonElement(useReflection, visited))
            put("third", third.toJsonElement(useReflection, visited))
        }

        else -> {
            if (useReflection) {
                reflectToJson(this, true, visited)
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
                        JsonPrimitive(this.toString())
                    }
                }
            }
        }
    }.also {
        if (this != null) {
            visited.remove(id)
        }
    }
}

fun reflectToJson(
    obj: Any,
    useReflection: Boolean,
    visited: MutableSet<Int>
): JsonElement {
    val kClass = obj::class
    val propsByName = kClass.memberProperties.associateBy { it.name }
    val ctorParams = kClass.primaryConstructor?.parameters

    return buildJsonObject {
        if (ctorParams != null) {
            for (param in ctorParams) {
                val name = param.name ?: continue
                val prop = propsByName[name] ?: continue

                val value = runCatching { prop.getter.call(obj) }.getOrNull()
                put(name, value.toJsonElement(useReflection, visited))
            }
        } else {
            propsByName.forEach { (name, prop) ->
                val value = runCatching { prop.getter.call(obj) }.getOrNull()
                put(name, value.toJsonElement(useReflection, visited))
            }
        }
    }
}
