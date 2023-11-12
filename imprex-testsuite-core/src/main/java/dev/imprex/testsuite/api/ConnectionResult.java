package dev.imprex.testsuite.api;

public enum ConnectionResult {
	/**
     * The player was successfully connected to the server.
     */
    SUCCESS,
    /**
     * The player is already connected to this server.
     */
    ALREADY_CONNECTED,
    /**
     * The connection is already in progress.
     */
    CONNECTION_IN_PROGRESS,
    /**
     * A plugin has cancelled this connection.
     */
    CONNECTION_CANCELLED,
    /**
     * The server disconnected the user. A reason may be provided in the {@link Result} object.
     */
    SERVER_DISCONNECTED
}
