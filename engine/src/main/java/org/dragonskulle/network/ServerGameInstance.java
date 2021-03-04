/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Random;

/** @author Oscar L The type Server game instance. */
public class ServerGameInstance {
    /** The Map store, should be depreciated in favour of a HexMap object. */
    private final byte[] mMap =
            new byte[600]; // This is a placeholder to hold the map, in reality a clone of the
    // current map will be made.

    /** Instantiates a new Server game instance. */
    ServerGameInstance() {
        new Random().nextBytes(mMap);
    }

    /**
     * Gets the bytes of the map.
     *
     * @return the bytes of the map
     */
    public byte[] cloneMap() {
        // TODO be replaced with HexMap.serialize();
        return mMap;
    }

    /**
     * True if the instance is setup.
     *
     * @return the boolean
     */
    public boolean isSetup() {
        return this.mMap != null;
    }
}