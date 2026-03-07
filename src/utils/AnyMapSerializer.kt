package utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Serialize `Map<String, Any>` to JSON in a strict manner,
 * where every fields are transformed into JSONElement.
 */
object AnyMapSerializerStrict : KSerializer<Map<String, Any>> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("This serializer only works with JSON")
        val converted = value.mapValues { (_, v) -> v.toJsonValue(prioritizeToString = false) }
        jsonEncoder.encodeJsonElement(JsonObject(converted))
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("This serializer only works with JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        return obj.mapValues { it.value }
    }
}

/**
 * Serialize `Map<String, Any>` to JSON in a non-strict manner,
 * where non-primitive or built-in types (e.g., user-defined objects)
 * are transformed into string representation.
 */
object AnyMapSerializerReadable : KSerializer<Map<String, Any>> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), JsonElement.serializer()).descriptor

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("This serializer only works with JSON")
        val converted = value.mapValues { (_, v) -> v.toJsonValue(prioritizeToString = true) }
        jsonEncoder.encodeJsonElement(JsonObject(converted))
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("This serializer only works with JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        return obj.mapValues { it.value }
    }
}
