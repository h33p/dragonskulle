/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.Capital.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.jetbrains.annotations.NotNull;

/**
 * The type Client game instance.
 *
 * @author Oscar L The type Client game instance, used to store all game data. It is used to spawn
 *     objects to the game scene if there is an active one linked. If there is not one linked it
 *     will spawn into its own store (mNetworkObjectReferences) so that it can be tested without the
 *     attached game.
 */
public class ClientGameInstance {
    private static final Logger mLogger = Logger.getLogger(ClientGameInstance.class.getName());
    /**
     * If the instance is attached to a game scene then it will spawn objects directly onto the
     * scene.
     */
    private Scene linkedScene;
    /** This flag is for marking if the instance is linked to a game scene. */
    private boolean isLinkedToScene = false;

    /**
     * Instantiates a new Client game instance, A callback is needed to send bytes to the server.
     * This constructor is used when there is no initially linked game scene.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback) {
        this.sendBytesCallback = callback;
    }

    /**
     * Instantiates a new Client game instance, A callback is needed to send bytes to the server.
     * This constructor is used when there game scene is needed to be linked from instantiation.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback, Scene mainScene) {
        this.sendBytesCallback = callback;
        this.linkToScene(mainScene);
    }

    /**
     * Spawns a networkable component locally. This will be generated from bytes. If the networkable
     * components owner doesn't exist, it will be created.
     *
     * @param ownerId the owner id
     * @param component the component to be spawned
     * @return the id of the component spawned
     */
    public int spawnComponent(int ownerId, NetworkableComponent component) {
        mLogger.info("Spawning component on game instance");
        Reference<NetworkObject> nobReference = getNetworkObject(ownerId);
        if (nobReference == null) {
            nobReference = spawnNewNetworkObject(ownerId);
        }
        nobReference.get().addNetworkableComponent(component);
        return component.getId();
    }

    /**
     * Links the instance to the game scene.
     *
     * @param mainScene the game scene to be linked
     */
    public void linkToScene(Scene mainScene) {
        mLogger.info("LINKED TO SCENE");
        this.isLinkedToScene = true;
        this.linkedScene = mainScene;
    }

    /** The interface for sending bytes to the server in the form of a callback. */
    public interface NetworkClientSendBytesCallback {
        /**
         * Send.
         *
         * @param bytes the bytes
         */
        void send(byte[] bytes);
    }

    /** The Map bytes recieved from the server. */
    private byte[] mMap;

    /**
     * The callback for sending bytes to the server, it is created on instantiation of the class.
     */
    public NetworkClientSendBytesCallback sendBytesCallback;

    /**
     * An array of references to objects. If there is no game linked then it will only be stored on
     * the instance, otherwise the reference will be stored on the game scene.
     */
    private final HashMap<Integer, Reference<NetworkObject>> mNetworkObjectReferences =
            new HashMap<>();
    /** True if the players capital has been spawned. */
    private Boolean mHasCapital = false;

    /**
     * Gets the network objects stored with this instance.
     *
     * @return the network objects
     */
    public HashMap<Integer, Reference<NetworkObject>> getNetworkObjects() {
        return mNetworkObjectReferences;
    }

    /**
     * Gets a networked component by id.
     *
     * @param id the Id of the component to retrieve
     * @return the networked component found, if null then the component doesn't exists
     */
    public Reference<NetworkableComponent> getNetworkedComponent(int id) {
        Reference<NetworkableComponent> found = null;
        for (Reference<NetworkObject> nob : getNetworkObjects().values()) {
            Reference<NetworkableComponent> nc = nob.get().findComponent(id);
            if (nc != null) {
                found = nc;
                break;
            }
        }
        return found;
    }

    /**
     * Gets a networked component by class.
     *
     * @param clazz the class of the component to retrieve
     * @return the networked component found, if null then the component doesn't exists
     */
    public Reference<NetworkableComponent> getNetworkedComponent(
            Class<NetworkableComponent> clazz) {
        Reference<NetworkableComponent> found = null;
        System.out.println("Trying to find noc by class " + clazz.getName());
        for (Reference<NetworkObject> nob : getNetworkObjects().values()) {
            System.out.println("nob checking in  :  " + nob.toString());
            Reference<NetworkableComponent> nc = nob.get().getComponent(clazz);
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
     * @param networkObjectId the id of the object
     * @return the network object found, if none exists then null.
     */
    public Reference<NetworkObject> getNetworkObject(int networkObjectId) {
        mLogger.fine(mNetworkObjectReferences.toString());
        Reference<NetworkObject> nob = mNetworkObjectReferences.get(networkObjectId);
        return nob;
    }

    /**
     * Spawns a new network object if a local owner doesn't exist. If it is linked to a scene it
     * will also link the spawned object to the scene, and spawn it in the scene.
     *
     * @param networkObjectId the network object id to be spawned
     * @return the reference to the new object
     */
    @NotNull
    private Reference<NetworkObject> spawnNewNetworkObject(int networkObjectId) {
        final NetworkObject nob = new NetworkObject(networkObjectId, false);
        mLogger.info("adding a new root object to the scene");
        mLogger.info("nob to be spawned is : " + nob.toString());
        if (isLinkedToScene) {
            nob.linkToScene();
            this.linkedScene.addRootObject(nob);
        }
        Reference<NetworkObject> ref = nob.getNetReference();
        this.mNetworkObjectReferences.put(nob.getId(), ref);
        return ref;
    }

    /**
     * Determines the state of the instance, if it is ready or not.
     *
     * @return true if ready, false if not.
     */
    public boolean isSetup() {
        return this.mMap != null;
    }

    /**
     * Spawn a capital locally.
     *
     * @param networkObjectId the id of the network object that owns this component.
     * @param capital the capital component to be spawned
     * @return the id of the spawned capital
     */
    public int spawnCapital(int networkObjectId, Capital capital) {
        Reference<NetworkObject> nob = getNetworkObject(networkObjectId);
        if (nob == null) {
            nob = spawnNewNetworkObject(networkObjectId);
        }
        mLogger.info("adding networkable to nob");
        if (isLinkedToScene) {
            nob.get().linkToScene();
        }
        nob.get().addNetworkableComponent(capital);
        //        this.mNetworkObjectReferences.add(nob.getNetReference());
        this.mHasCapital = true;
        return capital.getId();
    }

    /**
     * Spawns a map locally.
     *
     * @param spawnedMap the spawned map
     */
    public void spawnMap(byte[] spawnedMap) {
        this.mMap = spawnedMap;
    }

    /**
     * Updates a networkable object from server message.
     *
     * @param payload the payload of the object to be updated
     */
    public void updateNetworkObject(byte[] payload) {
        // 4 bytes will be allocated for the id
        int idToUpdate = NetworkObject.getIdFromBytes(payload);
        Reference<NetworkObject> networkObjectToUpdate = getNetworkObject(idToUpdate);
        if (networkObjectToUpdate == null) {
            mLogger.info("couldn't find nob id :" + idToUpdate);
            networkObjectToUpdate = this.spawnNewNetworkObject(idToUpdate);
        }
        try {
            networkObjectToUpdate.get().updateFromBytes(payload, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets networkable by the id of the component.
     *
     * @param id the id
     * @return the reference to the networkable component found
     */
    private Reference<NetworkableComponent> getNetworkable(int id) {
        for (Reference<NetworkObject> mNetworkedObject : getNetworkObjects().values()) {
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
     * Prints a networkable component.
     *
     * @param id the id of the component
     */
    public void printNetworkable(int id) {
        mLogger.fine(Objects.requireNonNull(getNetworkable(id)).toString());
    }

    /**
     * Function for testing
     *
     * @return Returns the status of the spawned map
     */
    public boolean hasSpawnedMap() {
        return this.mMap != null;
    }

    /** @return True if a captial has been spawned */
    public Boolean hasSpawnedCapital() {
        return this.mHasCapital;
    }
}
