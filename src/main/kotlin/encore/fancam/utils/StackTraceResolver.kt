package encore.fancam.utils

import encore.fancam.events.TraceElement
import encore.fancam.events.toTraceElement
import kotlin.jvm.optionals.getOrNull

class StackTraceResolver {
    private val walker = StackWalker.getInstance()

    fun resolve(): TraceElement? {
        return walker.walk { frames ->
            frames
                .dropWhile { isInternal(it.className) }
                .findFirst()
                .getOrNull()
        }?.toStackTraceElement()?.toTraceElement()
    }

    private fun isInternal(className: String): Boolean {
        return className.startsWith("encore.utils.logging") ||
                className.startsWith("encore.utils.StackTraceResolver") ||
                className.startsWith("java.lang.Thread") ||
                className.startsWith("java.util.concurrent")
    }
}