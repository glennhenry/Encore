package encore.network.lifecycle

import encore.context.ServerContext
import encore.fancam.Fancam
import encore.network.transport.Connection

/**
 * Dispatches hooks for [PlayerLifecycle] events.
 *
 * Callers can register hooks via [register], which will be invoked on:
 * - [PlayerLifecycle.OnConnect]
 * - [PlayerLifecycle.OnDisconnect]
 * - [PlayerLifecycle.OnSend]
 * - [PlayerLifecycle.OnReceive]
 *
 * Execution semantics:
 * - Hooks are invoked in the order they are registered.
 * - Each hook runs *after* the corresponding lifecycle event has completed:
 *     - `OnConnect`: after the player is connected
 *     - `OnDisconnect`: after the player is disconnected
 *     - `OnSend`: after a message is sent
 *     - `OnReceive`: after a non-empty message is received
 *
 * This component serves as an extension point over the connection lifecycle.
 * Hooks typically contains side-effect and not domain logic or handling.
 *
 * Typical use cases include:
 * - Updating player activity timestamps
 * - Marking players online/offline on connect/disconnect
 */
class PlayerLifecycleHandler(private val logEachHook: Boolean = true) {
    /**
     * Hook of player lifecycle, provides [ServerContext]
     * and the associated player's [Connection].
     */
    typealias LifecycleHook = (ServerContext, Connection) -> Unit

    private val onConnectHooks = mutableMapOf<String, LifecycleHook>()
    private val onDisconnectHooks = mutableMapOf<String, LifecycleHook>()
    private val onSendHooks = mutableMapOf<String, LifecycleHook>()
    private val onReceiveHooks = mutableMapOf<String, LifecycleHook>()

    /**
     * Register the [hook] identified by [name] for the [lifecycle].
     *
     * @param lifecycle The specific lifecycle that the hook is for.
     * @param name Human-readable identifier for debugging purpose.
     * @param hook The hook to be invoked.
     */
    fun register(lifecycle: PlayerLifecycle, name: String, hook: LifecycleHook) {
        when (lifecycle) {
            PlayerLifecycle.OnConnect -> onConnectHooks[name] = hook
            PlayerLifecycle.OnDisconnect -> onDisconnectHooks[name] = hook
            PlayerLifecycle.OnSend -> onSendHooks[name] = hook
            PlayerLifecycle.OnReceive -> onReceiveHooks[name] = hook
        }
    }

    /**
     * Represent the [PlayerLifecycle.OnConnect] event for [connection].
     */
    fun onConnect(serverContext: ServerContext, connection: Connection) {
        if (logEachHook) Fancam.trace { "onConnect lifecycle for '${connection.playerName}'" }
        for ((name, hook) in onConnectHooks) {
            try {
                hook(serverContext, connection)
                if (logEachHook) Fancam.trace { "onConnect '$name' executed" }
            } catch (e: Exception) {
                Fancam.error(e) { "onConnect '$name' error for '${connection.playerName}'" }
            }
        }
    }

    /**
     * Represent the [PlayerLifecycle.OnDisconnect] event for [connection].
     */
    fun onDisconnect(serverContext: ServerContext, connection: Connection) {
        if (logEachHook) Fancam.trace { "onDisconnect lifecycle for '${connection.playerName}'" }
        for ((name, hook) in onDisconnectHooks) {
            try {
                hook(serverContext, connection)
                if (logEachHook) Fancam.trace { "onDisconnect '$name' executed" }
            } catch (e: Exception) {
                Fancam.error(e) { "onDisconnect '$name' error for '${connection.playerName}'" }
            }
        }
    }

    /**
     * Represent the [PlayerLifecycle.OnSend] event for [connection].
     */
    fun onSend(serverContext: ServerContext, connection: Connection) {
        if (logEachHook) Fancam.trace { "onSend lifecycle for '${connection.playerName}'" }
        for ((name, hook) in onSendHooks) {
            try {
                hook(serverContext, connection)
                if (logEachHook) Fancam.trace { "onSend '$name' executed" }
            } catch (e: Exception) {
                Fancam.error(e) { "onSend '$name' error for '${connection.playerName}'" }
            }
        }
    }

    /**
     * Represent the [PlayerLifecycle.OnReceive] event for [connection].
     */
    fun onReceive(serverContext: ServerContext, connection: Connection) {
        if (logEachHook) Fancam.trace { "onReceive lifecycle for '${connection.playerName}'" }
        for ((name, hook) in onReceiveHooks) {
            try {
                hook(serverContext, connection)
                if (logEachHook) Fancam.trace { "onReceive '$name' executed" }
            } catch (e: Exception) {
                Fancam.error(e) { "onReceive '$name' error for '${connection.playerName}'" }
            }
        }
    }
}
