package annotation

/**
 * Marks a piece of code (e.g., a function or class) as untested.
 *
 * This annotation is typically used for critical business logic
 * that is expected to have unit tests but has not yet been covered.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Untested(val message: String = "")
