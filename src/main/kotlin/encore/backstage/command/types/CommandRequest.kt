package encore.backstage.command.types

import encore.serialization.AnyMapSerializer
import kotlinx.serialization.Serializable

/**
 * Represent a raw command request sent by the backstage tool.
 *
 * This model is produced from HTTP request, so it is unverified:
 * - The [commandId] may not correspond to the registered commands.
 * - The [arguments] isn't guaranteed to contain every required arguments.
 * - Each type of arguments isn't guaranteed to match the argument's expected type.
 */
@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(AnyMapSerializer::class)
data class CommandRequest(
    val commandId: String,
    val arguments: Map<String, Any>
)
