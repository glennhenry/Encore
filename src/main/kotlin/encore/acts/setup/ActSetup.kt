package encore.acts.setup

import encore.acts.StageAct

/**
 * Runtime configuration model of [StageAct].
 *
 * [ActSetup] describes the execution detail of a stage act.
 *
 * @property initialDelay The amount of delay carried out before the act
 *                        performs for the first time.
 * @property performMode Defines the execution portion.
 * @property lifetimeMode Defines the act's existence.
 */
data class ActSetup(
    val initialDelay: Long,
    val performMode: PerformMode,
    val lifetimeMode: LifetimeMode,
)
