package core.data

import core.data.resources.GameResource
import core.data.resources.GameResourcesParser
import io.ktor.util.date.*
import utils.logging.Logger
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

/**
 * Define the core definitions and rules that shapes the game.
 *
 * For each kind of game resource, an implementation of [GameResource]
 * and a parser [GameResourcesParser] is needed.
 *
 * Implementor can add data structure and indexes in this object and parser would populate it.
 */
object GameDefinition {
    val exampleFromResParser = mutableMapOf<String, Int>()

    /**
     * Initialize GameDefinition by reading all resources using the registered parsers.
     *
     * Here, implementor must list game resources to be loaded along with the parser.
     */
    fun initialize() {
        val startA = getTimeMillis()

        // REPLACE add
        val resources = listOf<GameResource>()
        val parsers: Map<KClass<out GameResource>, GameResourcesParser<*>> = mapOf()

        for (res in resources) {
            val start = getTimeMillis()
            val parser = parsers[res::class]
            if (parser == null) {
                Logger.warn { "No parser registered for resource type: ${res::class.simpleName}" }
                continue
            }

            @Suppress("UNCHECKED_CAST")
            (parser as GameResourcesParser<GameResource>).parse(res, this)

            Logger.info { "Finished parsing ${res.name} in ${(getTimeMillis() - start).milliseconds}" }
        }

        Logger.info { "All game resources loaded in ${(getTimeMillis() - startA).milliseconds}" }
    }

    /**
     * Reset game definition data and indexes.
     *
     * This function is only used during unit tests, to allow each parser
     * to populate `GameDefinition` without interfering each others.
     */
    fun reset() {
        GameDefinition.apply {
            exampleFromResParser.clear()
        }
    }
}
