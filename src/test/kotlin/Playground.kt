import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Playground for quick testing and code run
 *
 * .\gradlew run --tests "Playground.playground" --console=plain
 */
@Ignore
class Playground {
    @Test
    fun playground() {
        for (i in 0 until 257) {
            println("\u001B[48;5;${i}m THIS IS $i")
        }
        throw Exception()
    }
}
