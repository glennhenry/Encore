package encore.definition

/**
 * Parser for game resources.
 *
 * They should parse resources file to create code-level representation of the game's data.
 */
interface GameResourceParser<T> {
    /**
     * Parse the given resource of type [T] and populate [GameReference] as needed.
     */
    fun parse(res: T, gameDefinition: GameReference)
}
