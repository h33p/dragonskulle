/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.IClientListener;
import org.dragonskulle.network.NetworkClient;
import org.dragonskulle.network.components.NetworkManager.IObjectOwnerModifiedEvent;
import org.dragonskulle.network.components.NetworkManager.IObjectSpawnEvent;

/**
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class ClientNetworkManager {

    /** Describes client connection state */
    private static enum ConnectionState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        JOINED_GAME,
        CONNECTION_ERROR,
        CLEAN_DISCONNECTED
    }

    /** Client listener */
    private class Listener implements IClientListener {
        @Override
        public void unknownHost() {
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void couldNotConnect() {
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void serverClosed() {
            mNextConnectionState.set(ConnectionState.CLEAN_DISCONNECTED);
        }

        @Override
        public void disconnected() {
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void connectedToServer(int netID) {
            mNetID = netID;
            mNextConnectionState.set(ConnectionState.CONNECTED);
        }

        @Override
        public void error(String s) {
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        /**
         * Updates a networkable object from server message.
         *
         * @param payload the payload of the object to be updated
         */
        @Override
        public void updateNetworkObject(DataInputStream stream) throws IOException {
            int idToUpdate = stream.readInt();
            ClientObjectEntry entry = getNetworkObjectEntry(idToUpdate);
            if (entry == null) {
                log.info("Should have spawned! Couldn't find nob id :" + idToUpdate);
                return;
            }
            int oldOwner = entry.mNetworkObject.get().getOwnerId();
            int newOwner = entry.mNetworkObject.get().updateFromBytes(stream);
            if (oldOwner != newOwner) { // ownership has changed
                updateOwnershipLink(entry.mNetworkObject);
            }
            if (!entry.mSynchronized) {
                entry.mSynchronized = true;
                entry.mNetworkObject.get().getGameObject().setEnabled(true);
            }
        }

        /**
         * Update the server's state on the client.
         *
         * @param payload payload containing the server's world state
         */
        @Override
        public void updateServerState(DataInputStream stream) throws IOException {
            mServerTime = stream.readFloat();
        }

        private void updateOwnershipLink(Reference<NetworkObject> mNetworkObject) {
            mModifiedOwnerListeners.stream().forEach(l -> l.handleModifyOwner(mNetworkObject));
        }

        @Override
        public void spawnNetworkObject(DataInputStream stream) throws IOException {
            int objectId = stream.readInt();
            int ownerId = stream.readInt();
            int spawnTemplateId = stream.readInt();
            spawnNewNetworkObject(objectId, ownerId, spawnTemplateId);
        }

        @Override
        public void objectEvent(DataInputStream stream) throws IOException {
            int objectID = stream.readInt();
            ClientObjectEntry entry = getNetworkObjectEntry(objectID);
            if (entry == null) {
                log.info("Should have spawned! Couldn't find nob id :" + objectID);
                return;
            }
            NetworkObject nob = entry.mNetworkObject.get();

            int eventID = stream.readInt();

            if (nob != null) nob.handleServerEvent(eventID, stream);
        }
    }

    private static class ClientObjectEntry {
        private boolean mSynchronized;
        private final Reference<NetworkObject> mNetworkObject;

        public ClientObjectEntry(Reference<NetworkObject> networkObject) {
            mSynchronized = false;
            mNetworkObject = networkObject;
        }
    }

    /** Underlying network client instance */
    private final NetworkClient mClient;
    /** Client event callback listener */
    private final IClientListener mListener = new Listener();
    /** Current connection state */
    @Getter private ConnectionState mConnectionState = ConnectionState.NOT_CONNECTED;
    /** Next connection state (set by the listener) */
    private AtomicReference<ConnectionState> mNextConnectionState = new AtomicReference<>(null);
    /** Callback for connection result processing */
    private NetworkManager.IConnectionResultEvent mConnectionHandler;
    /** Back reference to the network manager */
    private final NetworkManager mManager;
    /** How many ticks elapsed without any updates */
    private int mTicksWithoutRequests = 0;
    /** Listeners for spawn events */
    private List<IObjectSpawnEvent> mSpawnListeners = new ArrayList<>();
    /** Listeners for owner modification events */
    private List<IObjectOwnerModifiedEvent> mModifiedOwnerListeners = new ArrayList<>();

    @Getter private int mNetID = -1;

    @Getter private float mServerTime = 0f;

    /** An map of references to objects. */
    private final HashMap<Integer, ClientObjectEntry> mNetworkObjectReferences = new HashMap<>();

    /**
     * Constructor for ClientNetworkManager
     *
     * @param manager target back reference to {@link NetworkManager}
     * @param ip target connection IP address
     * @param port target connection port
     * @param handler connection result callback
     */
    ClientNetworkManager(
            NetworkManager manager,
            String ip,
            int port,
            NetworkManager.IConnectionResultEvent handler) {
        mManager = manager;
        mConnectionState = ConnectionState.CONNECTING;
        mClient = new NetworkClient(ip, port, mListener);
        mConnectionHandler = handler;
    }

    /**
     * Send byte message to the server
     *
     * @param message message to send
     */
    public void sendToServer(byte[] message) {
        mClient.sendBytes(message);
    }

    public DataOutputStream getDataOut() {
        return mClient.getDataOut();
    }

    /**
     * Gets a network object by id.
     *
     * @param networkObjectId the id of the object
     * @return the network object found, if none exists then null.
     */
    public Reference<NetworkObject> getNetworkObject(int networkObjectId) {
        ClientObjectEntry entry = getNetworkObjectEntry(networkObjectId);
        return entry == null ? null : entry.mNetworkObject;
    }

    public void registerSpawnListener(IObjectSpawnEvent listener) {
        mSpawnListeners.add(listener);
    }

    public void unregisterSpawnListener(IObjectSpawnEvent listener) {
        mSpawnListeners.remove(listener);
    }

    public void registerOwnershipModificationListener(IObjectOwnerModifiedEvent listener) {
        mModifiedOwnerListeners.add(listener);
    }

    public void unregisterOwnershipModificationListener(IObjectOwnerModifiedEvent listener) {
        mModifiedOwnerListeners.remove(listener);
    }

    /**
     * Gets all network objects as a stream
     *
     * @return the network object found, if none exists then null.
     */
    public Stream<Reference<NetworkObject>> getNetworkObjects() {
        log.fine(mNetworkObjectReferences.toString());
        return mNetworkObjectReferences.values().stream().map(e -> e.mNetworkObject);
    }

    /**
     * Disconnect from the server
     *
     * <p>This method will disconnect from the server and tell {@link NetworkManager} about it.
     */
    public void disconnect() {
        Engine engine = Engine.getInstance();

        if (engine.getPresentationScene() == mManager.getGameScene()) {
            engine.loadPresentationScene(Scene.getActiveScene());
        }

        mConnectionState = ConnectionState.NOT_CONNECTED;
        mClient.dispose();

        mNetworkObjectReferences.values().stream()
                .map(e -> e.mNetworkObject)
                .filter(Reference::isValid)
                .map(Reference::get)
                .map(NetworkObject::getGameObject)
                .forEach(GameObject::destroy);
        mNetworkObjectReferences.clear();

        mManager.onClientDisconnect();
    }

    /** Network update method, called by {@link NetworkManager} */
    void networkUpdate() {
        ConnectionState nextState = mNextConnectionState.getAndSet(null);

        if (nextState != null) {
            log.info(nextState.toString());
            log.info(mConnectionState.toString());

            if (mConnectionState == ConnectionState.CONNECTING) {
                switch (nextState) {
                    case CONNECTED:
                        joinGame();
                        if (mConnectionHandler != null)
                            mConnectionHandler.handle(mManager.getGameScene(), mManager, mNetID);
                        break;
                    case CONNECTION_ERROR:
                        if (mConnectionHandler != null)
                            mConnectionHandler.handle(mManager.getGameScene(), mManager, -1);
                        disconnect();
                        break;
                    default:
                        break;
                }
            } else if (mConnectionState == ConnectionState.JOINED_GAME) {
                // TODO: handle lobby -> game transition here
                disconnect();
            }
        }

        if (mConnectionState == ConnectionState.JOINED_GAME) {
            if (mClient.processRequests() <= 0) {
                mTicksWithoutRequests++;
                if (mTicksWithoutRequests > 3200) {
                    disconnect();
                } else if (mTicksWithoutRequests == 1000) {
                    log.info("1000 ticks without updates! 2200 more till disconnect!");
                }
            } else mTicksWithoutRequests = 0;
        }

        mNetworkObjectReferences
                .entrySet()
                .removeIf(entry -> !entry.getValue().mNetworkObject.isValid());
    }

    // TODO: implement lobby
    // private void joinLobby() {}

    /** Join the game map */
    private void joinGame() {
        Engine engine = Engine.getInstance();

        mManager.createGameScene(false);

        if (engine.getPresentationScene() == Scene.getActiveScene()) {
            engine.loadPresentationScene(mManager.getGameScene());
        } else {
            engine.activateScene(mManager.getGameScene());
        }

        mConnectionState = ConnectionState.JOINED_GAME;
    }

    /**
     * Gets a network object by id.
     *
     * @param networkObjectId the id of the object
     * @return the network object found, if none exists then null.
     */
    private ClientObjectEntry getNetworkObjectEntry(int networkObjectId) {
        log.fine(mNetworkObjectReferences.toString());
        return mNetworkObjectReferences.get(networkObjectId);
    }

    /**
     * Spawn a new network object
     *
     * @param networkObjectId allocated object ID
     * @param ownerID network owner ID
     * @param templateId template ID
     */
    private void spawnNewNetworkObject(int networkObjectId, int ownerID, int templateId) {
        final GameObject go = mManager.getSpawnableTemplates().instantiate(templateId);
        final NetworkObject nob = new NetworkObject(networkObjectId, ownerID, false, mManager);
        go.addComponent(nob);
        Reference<NetworkObject> ref = nob.getReference(NetworkObject.class);
        log.info("adding a new root object to the scene");
        log.info("nob to be spawned is : " + nob.toString());
        go.setEnabled(false);
        mManager.getGameScene().addRootObject(go);
        this.mNetworkObjectReferences.put(nob.getId(), new ClientObjectEntry(ref));
        nob.networkInitialize();
        mSpawnListeners.stream().forEach(l -> l.handleSpawn(nob));
    }
}
