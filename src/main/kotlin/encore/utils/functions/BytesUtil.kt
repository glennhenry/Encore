package encore.utils.functions

fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean {
    if (this.size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

fun ByteArray.safeAsciiString(): String {
    return this.map {
        if (it in 32..126) it.toInt().toChar() else '?'
    }.joinToString("")
}

fun ByteArray.hexString(): String {
    return joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
