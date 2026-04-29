package encore.acts

import kotlin.coroutines.cancellation.CancellationException
import encore.tasks.ServerTaskDispatcher

/**
 * Describe the reason of why a [StageAct] is cancelled.
 */
enum class CancellationReason {
    /**
     * Cancellation via [ServerTaskDispatcher.stopTask] where acts are either
     * explicitly stopped by user or by application.
     *
     * This could be because:
     * - The `stopTask` is called due to certain request.
     * - The owner of the lifecycle is no longer alive
     *   (e.g., player disconnects, server shutdown).
     *
     * Persistent acts that are stopped from this route will be persisted
     * and can be continued later.
     */
    Stopped,

    /**
     * Cancellation The act is stopped and terminated.
     *
     * Persistent acts that are stopped from this route will **not** be persisted
     * and past entry will be **deleted**.
     *
     * Non-persistent acts are **guaranteed** to never receive this reason.
     */
    Killed,
}

/**
 * Cancellation exception for [StageAct] that is equal to [CancellationReason.Stopped].
 */
class StopCancellationException : CancellationException("Act was cancelled")

/**
 * Cancellation exception for [StageAct] that is equal to [CancellationReason.Killed].
 */
class KillCancellationException : CancellationException("Act was killed")

/**
 * Convert the following [CancellationException] into [CancellationReason].
 *
 * - [StopCancellationException] becomes [CancellationReason.Stopped]
 * - [KillCancellationException] becomes [CancellationReason.Killed]
 */
fun CancellationException.toCancellationReason(): CancellationReason {
    return when (this) {
        is StopCancellationException -> CancellationReason.Stopped
        is KillCancellationException -> CancellationReason.Killed
        else -> CancellationReason.Stopped
    }
}
