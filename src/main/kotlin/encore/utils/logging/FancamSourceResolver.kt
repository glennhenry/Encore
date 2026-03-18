package encore.utils.logging

import kotlin.jvm.optionals.getOrNull

interface FancamSourceResolver {
    fun resolve(): TraceElement?
}

class OfficialFancamSourceResolver : FancamSourceResolver {
    private val walker = StackWalker.getInstance()

    override fun resolve(): TraceElement? {
        return walker.walk { frames ->
            frames
                .dropWhile { isInternal(it.className) }
                .findFirst()
                .getOrNull()
        }?.toStackTraceElement()?.toTraceElement()
    }

    private fun isInternal(className: String): Boolean {
        return className.startsWith("encore.utils.logging") ||
                className.startsWith("java.lang.Thread") ||
                className.startsWith("java.util.concurrent")
    }
}

class DisabledFancamSourceResolver : FancamSourceResolver {
    override fun resolve(): TraceElement? = null
}
