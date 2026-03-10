package encore.utils

import encore.utils.logging.Logger
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XML utility to flatten XML file into a flat key-value map.
 *
 * See [flatten].
 *
 * @param enableLogging Whether to enable verbose logging on parsing.
 */
class XMLFlattener(private val enableLogging: Boolean = true) {
    /**
     * Flatten an XML structure into a flat key-value map.
     *
     * For instance:
     * ```xml
     * <parent enabled="true">
     *     <child>123</child>
     * </parent>
     * ```
     *
     * parsed to:
     * - `parent._enabled` = `true`
     * - `parent.child` = `123`
     */
    fun flatten(xml: String, rootName: String): Map<String, String> {
        if (enableLogging) {
            Logger.verbose { "XMLFlattener: parsing XML root='$rootName'" }
        }

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(InputSource(StringReader(xml)))

        val root = doc.documentElement
            ?: throw IllegalArgumentException("XML does not contain a root element")

        val result = parseNode(rootName, root)

        if (enableLogging) {
            Logger.verbose {
                "XMLFlattener: parsed ${result.size} entries from root='$rootName'"
            }
            Logger.verbose {
                result.entries
                    .sortedBy { it.key.length }
                    .joinToString("\n") { "${it.key} = ${it.value}" }
            }
        }

        return result
    }

    /**
     * Parse individual node, returning map of each XML path to the value.
     *
     * For instance:
     * ```xml
     * <parent enabled="true">
     *     ...child
     * </parent>
     * ```
     *
     * parsed to:
     * - `parent._enabled` = `true`
     */
    private fun parseNode(
        path: String,
        element: Element
    ): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()

        if (enableLogging) {
            Logger.verbose { "XMLFlattener: entering <$path>" }
        }

        val children = element.childNodes

        // attributes
        for (i in 0 until element.attributes.length) {
            val attr = element.attributes.item(i)

            val key = "$path._${attr.nodeName}"
            val value = attr.nodeValue

            if (enableLogging) {
                Logger.verbose { "XMLFlattener: attribute $key='$value'" }
            }

            result[key] = value
        }

        val elementChildren = (0 until children.length)
            .mapNotNull { children.item(it) as? Element }

        // leaf node value
        if (elementChildren.isEmpty()) {
            val value = element.textContent.trim()
            if (value.isNotEmpty()) {
                if (enableLogging) {
                    Logger.verbose { "XMLFlattener: value $path='$value'" }
                }
                result[path] = value
            }
        }

        // recurse children
        for (child in elementChildren) {
            val childPath = "$path.${child.nodeName}"

            if (enableLogging) {
                Logger.verbose { "XMLFlattener: descending -> $childPath" }
            }

            val childMap = parseNode(childPath, child)

            for ((k, v) in childMap) {
                if (k in result) {
                    throw IllegalStateException("Duplicate config key detected: $k")
                }
                result[k] = v
            }
        }

        return result
    }
}
