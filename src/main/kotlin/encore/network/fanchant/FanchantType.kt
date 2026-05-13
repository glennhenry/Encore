package encore.network.fanchant

/**
 * Represents the protocol identifier for a concrete [Fanchant] type.
 *
 * A [FanchantType] binds a string identifier to its associated
 * concrete [Fanchant] type, providing type-safe mapping between
 * protocol-level identifiers and materialized runtime fanchants.
 *
 * This prevents accidental mismatches where the string identifier
 * does not correspond to the expected concrete [Fanchant] type.
 *
 * For instance, a message with type "login" being accidentally materialized as
 * `MoveFanchant`, in which a `LoginHandler` registered for "login" type will accept
 * the mismatched class normally and crash during runtime type conversion.
 *
 * @param T The concrete [Fanchant] associated with this identifier.
 */
interface FanchantType<T : Fanchant> {
    /**
     * Declare the identifier or logical type of this fanchant.
     * This will be used to dispatch the fanchant to handler.
     */
    val id: String
}
