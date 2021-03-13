/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Random;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.NetworkObject;

/** @author Oscar L The Server game instance. */
public class ServerGameInstance {
    /** The Map store, should be depreciated in favour of a HexMap object. */
    private final byte[] mMap =
            new byte[600]; // This is a placeholder to hold the map, in reality a clone of the

    /** The game scene that it is linked to. */
    private Scene scene;

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

    /**
     * Spawns a network object on scene.
     *
     * @param networkObject the network object
     */
    public void spawnNetworkObjectOnScene(NetworkObject networkObject) {
        this.scene.addRootObject(networkObject.getGameObject());
    }

    /**
     * Sets the linked game scene.
     *
     * @param scene the scene
     */
    public void setScene(Scene scene) {
        this.scene = scene;
    }
}
