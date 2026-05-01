package encore.acts.director

import encore.acts.StageAct
import encore.acts.setup.LifetimeMode
import encore.datastore.collection.PlayerId
import encore.datastore.collection.ServerId
import kotlinx.coroutines.CoroutineScope

/**
 * Represent the scope to which a [StageAct] is bound to.
 *
 * A stage act is bound to an owner that determines its lifetime.
 * The owner can be a particular player or the server itself.
 *
 * For example, if an act is bound to a player and their connection is dead,
 * the act will seen as invalid, and thus cancelled. This is typically determined
 * through the owner's [coroutineScope].
 *
 * **Important:** The [ownerId] is required for acts with [LifetimeMode.PausedPersistent]
 * or [LifetimeMode.ContinuousPersistent]; as it is used for identification
 * in saving and resuming acts.
 */
interface ActScope {
    /**
     * The globally unique identifier of the [StageAct] owner.
     *
     * This should be assigned properly for acts with [LifetimeMode.PausedPersistent]
     * or [LifetimeMode.ContinuousPersistent].
     *
     * For player this should be [PlayerId].
     * Server-owned acts should use the [ServerId].
     */
    val ownerId: String

    /**
     * The coroutine scope the act will run on.
     */
    val coroutineScope: CoroutineScope
}
