package core.data.resources

import core.data.GameDefinition

/**
 * Parser for game resources.
 *
 * They should parse resources file to create code-level representation of the game's data.
 */
interface GameResourcesParser<T> {
    /**
     * Parse the given resource of type [T] and populate [GameDefinition] as needed.
     */
    fun parse(res: T, gameDefinition: GameDefinition)
}
