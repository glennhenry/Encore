package encore.route.guard

/**
 * Result returned by [SecurityGuard] and [AuthGuard].
 */
sealed class GuardResult {
    /**
     * The request is allowed to continue.
     */
    data object Welcome : GuardResult()

    /**
     * The request is denied with an optional reason [why].
     */
    data class GetOut(val why: String = "<unspecified>") : GuardResult()
}
