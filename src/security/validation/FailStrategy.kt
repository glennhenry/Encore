package security.validation

/**
 * Defines the strategy to apply when a validation check fails.
 *
 * Used by [ValidationScheme] to determine how the system should react
 * to invalid player states or actions during request validation.
 *
 * The level of severity here is only used for reference, please suit with the practical cases.
 */
enum class FailStrategy {
    /**
     * Cancel the current request or operation but keep the connection open.
     */
    Cancel,

    /**
     * Flag the player or request for review, then cancel the operation.
     */
    FlagAndCancel,

    /**
     * Typically, a fatal condition where player should be disconnected
     */
    Disconnect
}
