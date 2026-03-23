package encore.serialization

import encore.fancam.Fancam
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
object Protobuf {
    private var _protoBuf: ProtoBuf? = null
    val protoBuf: ProtoBuf
        get() = _protoBuf ?: error("Protobuf is not initialized. Call Protobuf.initialize() first.")

    fun initialize(protoBuf: ProtoBuf) {
        if (_protoBuf != null) {
            Fancam.warn { "Protobuf.initialize() called after initialization. Ignoring." }
            return
        }
        this._protoBuf = protoBuf
    }

    inline fun <reified T> encode(value: T): ByteArray {
        return protoBuf.encodeToByteArray<T>(value)
    }

    inline fun <reified T> encode(serializer: SerializationStrategy<T>, value: T): ByteArray {
        return protoBuf.encodeToByteArray(serializer, value)
    }

    inline fun <reified T> decode(value: ByteArray): T {
        return protoBuf.decodeFromByteArray<T>(value)
    }

    inline fun <reified T> decode(deserializer: DeserializationStrategy<T>, value: ByteArray): T {
        return protoBuf.decodeFromByteArray(deserializer, value)
    }
}
