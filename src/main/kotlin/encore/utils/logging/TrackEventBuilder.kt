package encore.utils.logging

import io.ktor.util.date.getTimeMillis

class TrackEventBuilder(
    private val name: String,
    private val onRecordCalled: (TrackEvent) -> Unit,
    private val onLogCalled: (TrackEvent) -> Unit,
) {
    private val data = mutableMapOf<String, Any>()
    private var tags: List<String> = emptyList()
    private var note: () -> String = { "" }
    private var route: String = "events"

    /**
     * Shorthand to set the `playerId` attribute in the data.
     */
    fun playerId(pid: String) = apply { data["playerId"] = pid }

    /**
     * Shorthand to set the `username` attribute in the data.
     */
    fun username(name: String) = apply { data["username"] = name }

    /**
     * Set the tags for this track event.
     */
    fun tags(vararg tags: String) = apply { this.tags = tags.toList() }

    /**
     * Set the note for this track event.
     */
    fun note(note: () -> String) = apply { this.note = note }

    /**
     * Set the file destination for this track event.
     */
    fun route(route: String) = apply { this.route = route }

    /**
     * Adds a key-value pair in this log entry.
     *
     * If the [key] already exists in the entry, it will be overwritten.
     */
    fun data(key: String, value: Any) = apply { data[key] = value }

    /**
     * Record the track event to the specified file by [route].
     *
     * This method doesn't finalize the DSL.
     */
    fun record() = onRecordCalled(create())

    /**
     * Log the track event while also finalizing the builder.
     */
    fun log() = onLogCalled(create())

    private fun create(): TrackEvent = TrackEvent(
        name = name,
        timestamp = getTimeMillis(),
        data = data,
        route = route,
        tags = tags,
        note = note
    )
}
