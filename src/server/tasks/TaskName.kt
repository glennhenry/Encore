package server.tasks

/**
 * Identifier for tasks implementation, typically a short code.
 */
@Suppress("unused")
sealed class TaskName(val code: String) {
    data object DummyName: TaskName("DN")
}
