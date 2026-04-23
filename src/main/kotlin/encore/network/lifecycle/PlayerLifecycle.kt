package encore.network.lifecycle

/**
 * Represent the lifecycle of player connection in the socket server.
 */
enum class PlayerLifecycle {
    /**
     * Player is connected to the socket server.
     */
    OnConnect,

    /**
     * Player is disconnected from the socket server.
     */
    OnDisconnect,

    /**
     * The socket server sends a message to the player.
     */
    OnSend,

    /**
     * The socket server receives a message from the player.
     */
    OnReceive
}
