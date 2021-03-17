/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.Capital.Capital;
import org.dragonskulle.network.components.NetworkObject;

/**
 * The type Client game instance.
 *
 * @author Oscar L The type Client game instance, used to store all game data. It is used to spawn
 *     objects to the game scene if there is an active one linked. If there is not one linked it
 *     will spawn into its own store (mNetworkObjectReferences) so that it can be tested without the
 *     attached game.
 */
@Accessors(prefix = "m")
public class ClientGameInstance {
    private static final Logger mLogger = Logger.getLogger(ClientGameInstance.class.getName());

    private static final int SPAWN_OBJECT_ID = 0;
    private static final int SPAWN_TEMPLATE_ID = SPAWN_OBJECT_ID + 4;

    /**
     * If the instance is attached to a game scene then it will spawn objects directly onto the
     * scene.
     */
    private Scene mLinkedScene;
    /** This flag is for marking if the instance is linked to a game scene. */
    @Getter private boolean mIsLinkedToScene = false;

    /**
     * Instantiates a new Client game instance, A callback is needed to send bytes to the server.
     * This constructor is used when there is no initially linked game scene.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback) {
        this.mSendBytesCallback = callback;
    }

    /**
     * Instantiates a new Client game instance, A callback is needed to send bytes to the server.
     * This constructor is used when there game scene is needed to be linked from instantiation.
     *
     * @param callback the callback
     */
    ClientGameInstance(NetworkClientSendBytesCallback callback, Scene mainScene) {
        this.mSendBytesCallback = callback;
        this.linkToScene(mainScene);
    }

    /**
     * Links the instance to the game scene.
     *
     * @param mainScene the game scene to be linked
     */
    public void linkToScene(Scene mainScene) {
        mLogger.info("LINKED TO SCENE");
        this.mIsLinkedToScene = true;
        this.mLinkedScene = mainScene;
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
    public NetworkClientSendBytesCallback mSendBytesCallback;

    /**
     * An map of references to objects. If there is no game linked then it will only be stored on
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
     * Gets a network object by id.
     *
     * @param networkObjectId the id of the object
     * @return the network object found, if none exists then null.
     */
    public Reference<NetworkObject> getNetworkObject(int networkObjectId) {
        mLogger.fine(mNetworkObjectReferences.toString());
        return mNetworkObjectReferences.get(networkObjectId);
    }

    private Reference<NetworkObject> spawnNewNetworkObject(int networkObjectId, int templateId) {
        final GameObject go = Templates.instantiate(templateId);
        final NetworkObject nob = new NetworkObject(networkObjectId, false);
        go.addComponent(nob);
        Reference<NetworkObject> ref = nob.getReference(NetworkObject.class);
        mLogger.info("adding a new root object to the scene");
        mLogger.info("nob to be spawned is : " + nob.toString());
        if (mIsLinkedToScene) {
            this.mLinkedScene.addRootObject(go);
        } else {
            // TODO: avoid this somehow
            nob.onAwake();
        }
        this.mNetworkObjectReferences.put(nob.getId(), ref);

        // TODO: remove this!
        if (!mHasCapital) mHasCapital = go.getComponent(Capital.class) != null;

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
            mLogger.info("Should have spawned! Couldn't find nob id :" + idToUpdate);
            return;
        }
        try {
            networkObjectToUpdate.get().updateFromBytes(payload, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void spawnNetworkObject(byte[] payload) {
        int objectId = NetworkObject.getIntFromBytes(payload, SPAWN_OBJECT_ID);
        int spawnTemplateId = NetworkObject.getIntFromBytes(payload, SPAWN_TEMPLATE_ID);
        spawnNewNetworkObject(objectId, spawnTemplateId);
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
