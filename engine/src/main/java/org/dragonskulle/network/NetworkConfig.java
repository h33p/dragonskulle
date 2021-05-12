/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/**
 * The Network config, more configuration can be moved here.
 *
 * @author Oscar L
 */
public class NetworkConfig {
    public static final int TERMINATE_BYTES_LENGTH = 10;

    /** Standard message codes. */
    public static class Codes {
        /** ID of object update message. */
        public static final byte MESSAGE_DISCONNECT = -1;
        /** ID of object update message. */
        public static final byte MESSAGE_UPDATE_OBJECT = 15;
        /** ID of spawn object message. */
        public static final byte MESSAGE_SPAWN_OBJECT = 16;
        /** ID of server state update. */
        public static final byte MESSAGE_UPDATE_STATE = 17;
        /** ID of server to client event. */
        public static final byte MESSAGE_SERVER_EVENT = 18;
        /** ID of client to server request message. */
        public static final byte MESSAGE_CLIENT_REQUEST = 21;
        /** ID of server starting game message. */
        public static final byte MESSAGE_HOST_STARTED = 24;
        /** ID of client to server loading finished message. */
        public static final byte MESSAGE_CLIENT_LOADED = 25;
    }

    /** The constant MAX_TRANSMISSION_SIZE. */
    static final int MAX_TRANSMISSION_SIZE = 512;

    /** How many client requests can we process in a given tick (per client). */
    public static final int MAX_CLIENT_REQUESTS = 32;

    /** Specific challenge bytes server will send. */
    public static final byte SERVER_HANDSHAKE_BYTE = 42;
    /** Specific challenge bytes client will send. */
    public static final byte CLIENT_HANDSHAKE_BYTE = 0x42;
}
