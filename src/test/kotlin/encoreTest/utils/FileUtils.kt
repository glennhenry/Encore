package encoreTest.utils

import java.io.File

fun String.toFile(filename: String): File {
    val name = filename.split(".")
    return File.createTempFile(name[0], name[1]).also { it.writeText(this) }
}
