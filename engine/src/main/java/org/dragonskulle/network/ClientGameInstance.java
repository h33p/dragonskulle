/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * The type Client game instance.
 *
 * @author Oscar L The type Client game instance, used to store all game data. This will be replaced
 *     by the game engines data.
 */
public class ClientGameInstance {
    private final Logger mLogger = Logger.getLogger(this.getClass().getName());


    /**
     * Instantiates a new Client game instance.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback) {
        this.sendBytesCallback = callback;
    }

    /**
     * Spawn component int.
     *
     * @param ownerId the owner id
     * @param component the component
     * @return the int
     */
    public int spawnComponent(int ownerId, NetworkableComponent component) {
        NetworkObject nob = getNetworkObject(ownerId);
        nob.addChild(component);
        return component.getId();
    }

    /** The interface Network client send bytes callback. */
    public interface NetworkClientSendBytesCallback {
        /**
         * Send.
         *
         * @param bytes the bytes
         */
        void send(byte[] bytes);
    }

    /** The Map. */
    private byte[] mMap;

    /** The Send bytes callback. */
    public NetworkClientSendBytesCallback sendBytesCallback;

    /** The Networked objects. */
    private final ArrayList<NetworkObject> mNetworkedObjects = new ArrayList<>();
    /** True if a capital has been spawned. */
    private Boolean mHasCapital = false;

    /**
     * Gets networke objects.
     *
     * @return the network objects
     */
    public ArrayList<NetworkObject> getNetworkObjects() {
        return mNetworkedObjects;
    }

    /**
     * Gets a networked component by id.
     *
     * @param id the Id of the component
     * @return the networked components
     */
    public NetworkableComponent getNetworkedComponent(int id) {
        NetworkableComponent found = null;
        for (NetworkObject nob : mNetworkedObjects) {
            NetworkableComponent nc = nob.findComponent(id);
            if (nc != null) {
                found = nc;
                break;
            }
        }
        return found;
    }

    /**
     * Gets a network object by id.
     *
     * @param networkObjectId the Id of the object
     * @return the network object
     */
    public NetworkObject getNetworkObject(int networkObjectId) {
        mLogger.info(mNetworkedObjects.toString());
        NetworkObject nob =
                mNetworkedObjects.stream()
                        .filter(e -> e.getId() == networkObjectId)
                        .findFirst()
                        .orElse(null);
        if (nob != null) {
            return nob;
        }

        //        mLogger.info("couldn't find nob id :" + networkObjectId);
        nob = new NetworkObject(networkObjectId);
        this.mNetworkedObjects.add(nob); //            mLogger.info("managed to add new nob");
        return nob;
    }

    /**
     * If the game instance is setup it will return true.
     *
     * @return the boolean
     */
    public boolean isSetup() {
        return this.mMap != null;
    }

    /**
     * Spawn a capital locally.
     *
     * @param networkObjectId the network object id
     * @param capital the capital
     * @return the int
     */
    public int spawnCapital(int networkObjectId, Capital capital) {
        NetworkObject nob = getNetworkObject(networkObjectId);
        nob.addChild(capital);
        this.mHasCapital = true;
        return capital.getId();
    }

    /**
     * Spawn a map locally.
     *
     * @param spawnedMap the spawned map
     */
    public void spawnMap(byte[] spawnedMap) {
        this.mMap = spawnedMap;
    }

    /**
     * Update networkable from server message.
     *
     * @param payload the payload
     */
    public void updateNetworkObject(byte[] payload) {
        // 4 bytes will be allocated for the id
        int idToUpdate = NetworkObject.getIdFromBytes(payload);
        NetworkObject networkObjectToUpdate = getNetworkObject(idToUpdate);
        if (networkObjectToUpdate != null) {
            try {
                mLogger.info("BEFORE UPDATE");
                mLogger.info(networkObjectToUpdate.toString());
                networkObjectToUpdate.updateFromBytes(payload, this);
                mLogger.info("AFTER UPDATE");
                mLogger.info(networkObjectToUpdate.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mLogger.info("didnt find network object from id");
        }
    }

    /**
     * Gets networkable by the id of the component.
     *
     * @param id the id
     * @return the networkable
     */
    private NetworkableComponent getNetworkable(int id) {
        for (NetworkObject mNetworkedObject : mNetworkedObjects) {
            for (NetworkableComponent networkedComponent : mNetworkedObject.getChildren()) {
                if (networkedComponent.getId() == id) {
                    return networkedComponent;
                }
            }
        }
        return null;
    }

    /**
     * Print networkable.
     *
     * @param id the id of the component
     */
    public void printNetworkable(int id) {
        mLogger.info(Objects.requireNonNull(getNetworkable(id)).toString());
    }

    /**
     * Function for testing
     *
     * @return Returns the status of the spawned map
     */
    public boolean hasSpawnedMap() {
        return this.mMap != null;
    }

    /**
     * Has spawned capital boolean.
     *
     * @return the boolean
     */
    public Boolean hasSpawnedCapital() {
        return this.mHasCapital;
    }
}
