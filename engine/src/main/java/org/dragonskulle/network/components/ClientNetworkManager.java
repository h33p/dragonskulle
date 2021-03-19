/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

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
        public void updateNetworkObject(byte[] payload) {
            // 4 bytes will be allocated for the id
            int idToUpdate = NetworkObject.getIdFromBytes(payload);
            ClientObjectEntry entry = getNetworkObjectEntry(idToUpdate);
            if (entry == null) {
                log.info("Should have spawned! Couldn't find nob id :" + idToUpdate);
                return;
            }
            try {
                entry.mNetworkObject.get().updateFromBytes(payload);
                if (!entry.mSynchronized) {
                    entry.mSynchronized = true;
                    entry.mNetworkObject.get().getGameObject().setEnabled(true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void spawnNetworkObject(byte[] payload) {
            int objectId = NetworkObject.getIntFromBytes(payload, SPAWN_OBJECT_ID);
            int ownerId = NetworkObject.getIntFromBytes(payload, SPAWN_OWNER_ID);
            int spawnTemplateId = NetworkObject.getIntFromBytes(payload, SPAWN_TEMPLATE_ID);
            spawnNewNetworkObject(objectId, ownerId, spawnTemplateId);
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

    private static final int SPAWN_OBJECT_ID = 0;
    private static final int SPAWN_OWNER_ID = SPAWN_OBJECT_ID + 4;
    private static final int SPAWN_TEMPLATE_ID = SPAWN_OWNER_ID + 4;

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

    @Getter private int mNetID = -1;

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

        mManager.onClientDisconnect();
    }

    /** Network update method, called by {@link NetworkManager} */
    void networkUpdate() {
        ConnectionState nextState = mNextConnectionState.getAndSet(null);

        if (nextState != null) {
            System.out.println(nextState.toString());
            System.out.println(mConnectionState.toString());

            if (mConnectionState == ConnectionState.CONNECTING) {
                switch (nextState) {
                    case CONNECTED:
                        joinGame();
                        if (mConnectionHandler != null) mConnectionHandler.handle(mManager, mNetID);
                        break;
                    case CONNECTION_ERROR:
                        if (mConnectionHandler != null) mConnectionHandler.handle(mManager, -1);
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
    }

    // TODO: implement lobby
    // private void joinLobby() {}

    /** Join the game map */
    private void joinGame() {
        Engine engine = Engine.getInstance();

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
