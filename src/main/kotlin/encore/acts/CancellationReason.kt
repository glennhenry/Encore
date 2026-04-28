package encore.acts

/**
 * Describe the reason of why a [StageAct] is cancelled.
 */
enum class CancellationReason {
    /**
     * The normal path of explicit cancellation.
     */
    Manual,

    /**
     * The act is cancelled because the player is disconnected.
     */
    Disconnect,

    /**
     * The act is cancelled due to some error during it's execution.
     */
    Error
}
