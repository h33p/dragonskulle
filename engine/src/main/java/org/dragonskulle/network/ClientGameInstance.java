/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.Capital.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * The type Client game instance.
 *
 * @author Oscar L The type Client game instance, used to store all game data. This will be replaced
 *     by the game engines data.
 */
public class ClientGameInstance {
    private static final Logger mLogger = Logger.getLogger(ClientGameInstance.class.getName());
    private Scene linkedScene;
    private boolean isLinkedToScene = false;

    /**
     * Instantiates a new Client game instance.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback) {
        this.sendBytesCallback = callback;
    }

    /**
     * Instantiates a new Client game instance that is linked to the scene.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback, Scene mainScene) {
        this.sendBytesCallback = callback;
        this.linkToScene(mainScene);
    }

    /**
     * Spawn component int.
     *
     * @param ownerId the owner id
     * @param component the component
     * @return the int
     */
    public int spawnComponent(int ownerId, NetworkableComponent component) {
        mLogger.warning("Spawning component on game instance");
        NetworkObject nob = getNetworkObject(ownerId).get();
        nob.addNetworkableComponent(component);
        return component.getId();
    }

    public void linkToScene(Scene mainScene) {
        mLogger.warning("LINKED TO SCENE");
        this.isLinkedToScene = true;
        this.linkedScene = mainScene;
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
    private final ArrayList<Reference<NetworkObject>> mNetworkObjectReferences = new ArrayList<>();
    /** True if a capital has been spawned. */
    private Boolean mHasCapital = false;

    /**
     * Gets networke objects.
     *
     * @return the network objects
     */
    public ArrayList<Reference<NetworkObject>> getNetworkObjects() {
        return mNetworkObjectReferences;
    }

    /**
     * Gets a networked component by id.
     *
     * @param id the Id of the component
     * @return the networked components
     */
    public Reference<NetworkableComponent> getNetworkedComponent(int id) {
        Reference<NetworkableComponent> found = null;
        for (Reference<NetworkObject> nob : mNetworkObjectReferences) {
            Reference<NetworkableComponent> nc = nob.get().findComponent(id);
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
    public Reference<NetworkObject> getNetworkObject(int networkObjectId) {
        mLogger.info(mNetworkObjectReferences.toString());
        Reference<NetworkObject> nob =
                mNetworkObjectReferences.stream()
                        .filter(e -> e.get().getId() == networkObjectId)
                        .findFirst()
                        .orElse(null);
        if (nob != null) {
            return nob;
        }

        mLogger.warning("couldn't find nob id :" + networkObjectId);
        nob = this.spawnNewNetworkObject(networkObjectId);
        //        new NetworkObject(networkObjectId);
        //        this.mNetworkObjectReferences.add(nob); //            mLogger.info("managed to add
        // new nob");
        return nob;
    }

    private Reference<NetworkObject> spawnNewNetworkObject(int networkObjectId) {
        final NetworkObject nob = new NetworkObject(networkObjectId, false);
        mLogger.warning("adding a new root object to the scene");
        mLogger.warning("nob to be spawned is : " + nob.toString());
        if (isLinkedToScene) {
            nob.linkToScene();
            this.linkedScene.addRootObject(nob);
        }
        Reference<NetworkObject> ref = nob.getNetReference();
        this.mNetworkObjectReferences.add(ref);
        return ref;
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
        NetworkObject nob = getNetworkObject(networkObjectId).get();
        mLogger.warning("adding networkable to nob");
        nob.linkToScene();
        nob.addNetworkableComponent(capital);
        //        nob.addComponent(CapitalRenderable.get());
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
        NetworkObject networkObjectToUpdate = getNetworkObject(idToUpdate).get();
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
    private Reference<NetworkableComponent> getNetworkable(int id) {
        for (Reference<NetworkObject> mNetworkedObject : mNetworkObjectReferences) {
            for (Reference<NetworkableComponent> networkedComponent :
                    mNetworkedObject.get().getNetworkableChildren()) {
                if (networkedComponent.get().getId() == id) {
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
