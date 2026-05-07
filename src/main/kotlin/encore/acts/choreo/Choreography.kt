package encore.acts.choreo

import encore.acts.StageAct
import encore.utils.DayOfWeek
import encore.utils.TimeOfDay

/**
 * Defines how a [StageAct] is scheduled over time.
 *
 * This determines *when* an act should perform, but not *what* it does.
 *
 * Implementations:
 * - [BasicChoreography]: Covers simple runtime scheduling based on an initial delay
 *   and a fixed perform pattern (once, repeat, or forever).
 * - [DailyChoreography]: Schedules perform at a fixed [TimeOfDay] every day.
 * - [WeeklyChoreography]: Schedules perform at a fixed [DayOfWeek] and [TimeOfDay].
 *
 * For more advanced or dynamic scheduling behavior, implement [CustomChoreography].
 */
interface Choreography
