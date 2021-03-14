/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/** @author Oscar L The Network config, more configuration van be moved here. */
public class NetworkConfig {
    public static final int TERMINATE_BYTES_LENGTH = 10;

    public static class Codes {
        /** ID of object update message */
        public static final byte MESSAGE_UPDATE_OBJECT = 15;
        /** ID of spawn object message */
        public static final byte MESSAGE_SPAWN_OBJECT = 16;
        /** ID of spawn map message */
        public static final byte MESSAGE_SPAWN_MAP = 20;
        /** ID of client to server request message */
        public static final byte MESSAGE_CLIENT_REQUEST = 21;
    }

    /** The constant MAX_TRANSMISSION_SIZE. */
    static final int MAX_TRANSMISSION_SIZE = 512;
}
