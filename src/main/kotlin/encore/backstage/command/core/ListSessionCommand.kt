package encore.backstage.command.core

import basicDateFormatter
import encore.backstage.command.Command
import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.CommandResult
import encore.session.SessionSubunit
import encore.time.TimeCenter
import encore.utils.identifier.Ids
import encore.utils.identifier.shortUuid
import game.context.ServerContext

class ListSessionCommand : Command {
    override val id: String = "list-session"
    override val name: String = "List sessions"
    override val description: String = """
        List all valid sessions (results are limited to 20). 
        All IDs are short-uuid (first 8 chars).
    """.trimIndent()
    override val arguments: List<Argument> = emptyList()

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentInputs
    ): CommandResult {
        val sb = StringBuilder()

        serverContext.subunits.session.create(Ids.uuid())
        serverContext.subunits.session.create(Ids.uuid())
        serverContext.subunits.session.create(Ids.uuid())

        val sessions = serverContext.subunits.session.getAllActiveSessions()
        if (sessions.isEmpty()) {
            sb.appendLine("No sessions.")
        }

        sessions
            .take(20)
            .forEachIndexed { idx, ses ->
                sb.appendLine("${idx + 1}) ${formatSessionInfo(ses)}")
                sb.appendLine()
            }

        return CommandResult.Success(sb.toString())
    }
}

/**
 * Format [SessionInfo] into human-readable list.
 */
fun formatSessionInfo(ses: SessionInfo): String {
    val isExpired = if (TimeCenter.now() > ses.expiresAt) {
        "yes"
    } else {
        "no"
    }
    return """
        id=${ses.id.shortUuid()}
           issuedAt=${basicDateFormatter.format(ses.issuedAt)}
           expiresAt=${basicDateFormatter.format(ses.expiresAt)}
           isExpired=$isExpired
    """.trimIndent()
}

/**
 * Session info from [SessionSubunit].
 *
 * @property id userId
 * @property issuedAt
 * @property expiresAt
 */
data class SessionInfo(
    val id: String,
    val issuedAt: Long,
    val expiresAt: Long
)
