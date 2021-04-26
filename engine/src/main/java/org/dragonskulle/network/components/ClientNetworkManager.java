/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.SingletonStore;
import org.dragonskulle.network.IClientListener;
import org.dragonskulle.network.NetworkClient;
import org.dragonskulle.network.NetworkConfig;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client side network manager.
 *
 * @author Aurimas Blažulionis
 * @author Oscar L
 *     <p>This network manager exists on {@link NetworkManager} if, and only if there is a client
 *     game instance spawned on it. It contains the current game state, and spawned objects. It
 *     keeps track of game state, and communicates with the server.
 */
@Accessors(prefix = "m")
@Log
public class ClientNetworkManager {

    /** Describes client connection state. */
    private enum ConnectionState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        JOINING_GAME,
        JOINED_GAME,
        CONNECTION_ERROR,
        CLEAN_DISCONNECTED
    }

    /** Client listener. */
    private class Listener implements IClientListener {
        @Override
        public void unknownHost() {
            log.info("unknown host");
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void couldNotConnect() {
            log.info("could not connect");
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void serverClosed() {
            mNextConnectionState.set(ConnectionState.CLEAN_DISCONNECTED);
        }

        @Override
        public void disconnected() {
            log.info("disconnected");
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void connectedToServer(int netId) {
            mNetId = netId;
            mNextConnectionState.set(ConnectionState.CONNECTED);
        }

        @Override
        public void hostStartedGame() {
            mNextConnectionState.set(ConnectionState.JOINING_GAME);
        }

        @Override
        public void error(String s) {
            log.info("error: " + s);
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        /**
         * Updates a networkable object from server message.
         *
         * @param stream the payload of the object to be updated
         */
        @Override
        public void updateNetworkObject(DataInputStream stream) throws IOException {
            int idToUpdate = stream.readInt();
            ClientObjectEntry entry = getNetworkObjectEntry(idToUpdate);
            if (entry == null) {
                log.info("Should have spawned! Couldn't find nob id :" + idToUpdate);
                return;
            }
            entry.mNetworkObject.get().updateFromBytes(stream);
            if (!entry.mSynchronized) {
                entry.mSynchronized = true;
                entry.mNetworkObject.get().getGameObject().setEnabled(true);
            }
        }

        /**
         * Update the server's state on the client.
         *
         * @param stream payload containing the server's world state
         */
        @Override
        public void updateServerState(DataInputStream stream) throws IOException {
            mServerTime = stream.readFloat();
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
            int objectId = stream.readInt();
            ClientObjectEntry entry = getNetworkObjectEntry(objectId);
            if (entry == null) {
                log.info("Should have spawned! Couldn't find nob id :" + objectId);
                return;
            }
            NetworkObject nob = entry.mNetworkObject.get();

            int eventId = stream.readInt();

            if (nob != null) {
                nob.handleServerEvent(eventId, stream);
            }
        }
    }

    /** Internal entry for a client-side networked object. */
    private static class ClientObjectEntry {
        /**
         * Whether the object has been synchronized. Until this value is {@code true}, the object
         * will be disabled.
         */
        private boolean mSynchronized;
        /** Reference to the actual network object. */
        private final Reference<NetworkObject> mNetworkObject;

        /**
         * Constructor for {@link ClientObjectEntry}.
         *
         * @param networkObject reference to the network object in question
         */
        public ClientObjectEntry(Reference<NetworkObject> networkObject) {
            mSynchronized = false;
            mNetworkObject = networkObject;
        }
    }

    /** Underlying network client instance. */
    private final NetworkClient mClient;
    /** Client event callback listener. */
    private final IClientListener mListener = new Listener();
    /** Current connection state. */
    @Getter private ConnectionState mConnectionState;
    /** Next connection state (set by the listener). */
    private final AtomicReference<ConnectionState> mNextConnectionState =
            new AtomicReference<>(null);
    /** Callback for connection result processing. */
    private final NetworkManager.IConnectionResultEvent mConnectionHandler;
    /** Callback for when host has started game. */
    private final NetworkManager.IHostStartedGameEvent mHostStartedHandler;
    /** Back reference to the network manager. */
    private final NetworkManager mManager;
    /** How many ticks elapsed without any updates. */
    private int mTicksWithoutRequests = 0;

    @Getter private int mNetId = -1;

    @Getter private float mServerTime = 0f;

    /** An map of references to objects. */
    private final HashMap<Integer, ClientObjectEntry> mNetworkObjectReferences = new HashMap<>();

    /** Stores per-owner singletons. Can be looked up with getIdSingletons */
    private final HashMap<Integer, SingletonStore> mIdSingletons = new HashMap<>();

    /**
     * Constructor for ClientNetworkManager.
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
            NetworkManager.IConnectionResultEvent handler,
            NetworkManager.IHostStartedGameEvent startHandler) {
        mManager = manager;
        mConnectionState = ConnectionState.CONNECTING;
        mClient = new NetworkClient(ip, port, mListener);
        mConnectionHandler = handler;
        mHostStartedHandler = startHandler;
    }

    /**
     * Send byte message to the server.
     *
     * @param message message to send
     */
    public void sendToServer(byte[] message) {
        mClient.sendBytes(message);
    }

    /**
     * Get data output stream.
     *
     * <p>This will get a stream for client to server communication.
     *
     * @return output stream used to send data to the server. This is a wrapped stream which will
     *     append message length at the front of the message before sending. The message gets sent
     *     only when the stream gets closed.
     */
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

    /**
     * Gets all network objects as a stream.
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
        mConnectionState = ConnectionState.NOT_CONNECTED;
        mClient.dispose();

        mNetworkObjectReferences.values().stream()
                .map(e -> e.mNetworkObject)
                .filter(Reference::isValid)
                .map(Reference::get)
                .map(NetworkObject::getGameObject)
                .forEach(GameObject::destroy);
        mNetworkObjectReferences.clear();
    }

    /**
     * Get singletons for a object owner.
     *
     * @param ownerId owner of the singletons
     * @return singleton store for the given owner ID. If the store does not exist, a new one gets
     *     created.
     */
    public SingletonStore getIdSingletons(int ownerId) {
        Integer id = ownerId;
        SingletonStore store = mIdSingletons.get(id);
        if (store == null) {
            store = new SingletonStore();
            mIdSingletons.put(id, store);
        }
        return store;
    }

    /** Network update method, called by {@link NetworkManager}. */
    void networkUpdate() {
        int requestsProcessed = 0;
        switch (mConnectionState) {
            case CONNECTED:
                requestsProcessed = mClient.processRequests();
                break;
            case JOINED_GAME:
                requestsProcessed = mClient.processAllRequests();
                break;
            default:
                break;
        }

        if (requestsProcessed <= 0) {
            mTicksWithoutRequests++;
            if (mTicksWithoutRequests > 3200) {
                disconnect();
            } else if (mTicksWithoutRequests == 1000) {
                log.info("1000 ticks without updates! 2200 more till disconnect!");
            }
        } else mTicksWithoutRequests = 0;

        mNetworkObjectReferences
                .entrySet()
                .removeIf(entry -> !Reference.isValid(entry.getValue().mNetworkObject));
    }

    /**
     * Late network update method, called by {@link NetworkManager}.
     *
     * <p>This stage is used to handle state changes (scene changes, disconnections, etc.)
     */
    void lateNetworkUpdate() {
        ConnectionState nextState = mNextConnectionState.getAndSet(null);

        if (nextState != null) {
            log.info(nextState.toString());
            log.info(mConnectionState.toString());

            if (mConnectionState == ConnectionState.CONNECTING) {
                switch (nextState) {
                    case CONNECTED:
                        joinLobby();
                        if (mConnectionHandler != null) {
                            mConnectionHandler.handle(mManager, mNetId);
                        }
                        break;
                    case CONNECTION_ERROR:
                        if (mConnectionHandler != null) {
                            mConnectionHandler.handle(mManager, -1);
                        }
                        disconnect();
                        break;
                    default:
                        break;
                }
            } else if (mConnectionState == ConnectionState.CONNECTED) {
                if (nextState == ConnectionState.JOINING_GAME) {
                    joinGame();
                    if (mHostStartedHandler != null) {
                        mHostStartedHandler.handle(mManager.getGameScene(), mManager, mNetId);
                    }
                }
            } else if (mConnectionState == ConnectionState.JOINED_GAME) {
                disconnect();
            }
        }

        if (mConnectionState == ConnectionState.NOT_CONNECTED) {
            Engine engine = Engine.getInstance();

            if (engine.getPresentationScene() == mManager.getGameScene()) {
                engine.loadPresentationScene(Scene.getActiveScene());
            }

            mManager.onClientDisconnect();
        }
    }

    private void joinLobby() {
        mConnectionState = ConnectionState.CONNECTED;

        mManager.createGameScene(false);
    }

    /** Join the game map. */
    private void joinGame() {
        Engine engine = Engine.getInstance();

        // mManager.createGameScene(false);

        if (engine.getPresentationScene() == Scene.getActiveScene()) {
            engine.loadPresentationScene(mManager.getGameScene());
        } else {
            engine.activateScene(mManager.getGameScene());
        }

        mConnectionState = ConnectionState.JOINED_GAME;
        mClient.sendBytes(new byte[] {NetworkConfig.Codes.MESSAGE_CLIENT_LOADED});
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
     * Spawn a new network object.
     *
     * @param networkObjectId allocated object ID
     * @param ownerID network owner ID
     * @param templateId template ID
     */
    private void spawnNewNetworkObject(int networkObjectId, int ownerID, int templateId) {
        final GameObject go = mManager.getSpawnableTemplates().instantiate(templateId);

        if (go == null) {
            log.warning("Failed to instantiate template ID " + templateId);
            return;
        }

        go.getTransform()
                .setLocal3DTransformation(new Vector3f(), new Quaternionf(), new Vector3f(1));
        final NetworkObject nob = new NetworkObject(networkObjectId, ownerID, false, mManager);
        go.addComponent(nob);
        Reference<NetworkObject> ref = nob.getReference(NetworkObject.class);
        log.info("adding a new root object to the scene");
        log.info("nob to be spawned is : " + nob.toString());
        go.setEnabled(false);
        mManager.getGameScene().addRootObject(go);
        this.mNetworkObjectReferences.put(nob.getId(), new ClientObjectEntry(ref));
        nob.networkInitialize();
    }
}
