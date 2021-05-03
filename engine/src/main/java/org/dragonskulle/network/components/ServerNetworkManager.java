/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.Scene.SceneOverride;
import org.dragonskulle.core.SingletonStore;
import org.dragonskulle.network.IServerListener;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.Server;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.requests.ServerEvent.EventRecipients;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Server network manager.
 *
 * @author Aurimas Blažulionis
 *     <p>This class is composed in NetworkManager, and handles all things server.
 */
@Accessors(prefix = "m")
@Log
public class ServerNetworkManager {

    public enum ServerGameState {
        IN_PROGRESS,
        LOBBY,
        STARTING,
        NONE
    }

    /** Server event listener. */
    public class Listener implements IServerListener {
        /**
         * Client connected event.
         *
         * @param client the client
         * @return which network ID was allocated for the client
         */
        @Override
        public int clientConnected(ServerClient client) {
            return mServer.addConnectedClient(client);
        }

        @Override
        public void clientLoaded(ServerClient client) {
            client.setInGame(true);
            mClientLoadedEvent.handle(mManager.getGameScene(), mManager, client);
        }

        /**
         * Client disconnected event.
         *
         * @param client the client
         */
        @Override
        public void clientDisconnected(ServerClient client) {
            mServer.onClientDisconnect(client);
        }

        @Override
        public void clientComponentRequest(
                ServerClient client, int objectID, int requestID, DataInputStream stream)
                throws IOException {
            ServerObjectEntry entry = mNetworkObjects.get(objectID);

            if (entry == null) {
                log.info(
                        "Client "
                                + client.getNetworkID()
                                + " passed incorrect object ID: "
                                + objectID);
                return;
            }

            NetworkObject obj = entry.mNetworkObject.get();

            if (obj == null) {
                log.fine(
                        "Client "
                                + client.getNetworkID()
                                + " made a request on already destroyed object: "
                                + objectID);
                return;
            }

            if (obj.getOwnerId() != client.getNetworkID()) {
                log.severe(
                        "Client "
                                + client.getNetworkID()
                                + " made a request on an object not owned by them: "
                                + objectID
                                + " actual owner: "
                                + obj.getOwnerId());
                return;
            }

            if (!obj.handleClientRequest(requestID, stream)) {
                log.warning(
                        "Client "
                                + client.getNetworkID()
                                + " passed invalid request: "
                                + requestID
                                + " on object: "
                                + objectID);
            }
        }
    }

    public static class ServerObjectEntry {
        @Getter private final Reference<NetworkObject> mNetworkObject;
        @Getter private final int mTemplateId;
        @Getter private final Set<ServerClient> mSpawnedFor;
        private boolean mAwoken = false;

        public ServerObjectEntry(Reference<NetworkObject> networkObject, int templateId) {
            mNetworkObject = networkObject;
            mTemplateId = templateId;
            mSpawnedFor = new HashSet<>();
        }

        public void updateClient(ServerClient client) {

            NetworkObject obj = mNetworkObject.get();

            if (obj == null) {
                return;
            }

            if (!mAwoken) {
                obj.beforeNetSerialize();
                mAwoken = true;
            }

            boolean forceUpdate = false;

            // Send a spawn message to the client, if haven't already
            if (mSpawnedFor.add(client)) {
                try (DataOutputStream stream = client.getDataOut()) {
                    stream.writeByte(NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT);
                    stream.writeInt(obj.getNetworkObjectId());
                    stream.writeInt(obj.getOwnerId());
                    stream.writeInt(mTemplateId);
                    forceUpdate = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    client.closeSocket();
                }
            }

            obj.sendUpdate(client, forceUpdate);
        }
    }

    /** Server event listener. */
    private final Listener mListener = new Listener();
    /** Underlying server instance. */
    private Server mServer;
    /** Back reference to {@link NetworkManager}. */
    @Getter private final NetworkManager mManager;
    /** Callback for connected clients. */
    private final NetworkManager.IClientLoadedEvent mClientLoadedEvent;
    /** Callback for game start. */
    private final NetworkManager.IGameStartEvent mGameStartEventHandler;
    /** Callback for game end. */
    private final NetworkManager.IGameEndEvent mGameEndEventHandler;
    /** The Counter used to assign objects a unique id. */
    private final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);
    /** Describes the current state of the game. */
    private ServerGameState mGameState = ServerGameState.LOBBY;

    /**
     * The Network objects - this can be moved to game instance but no point until game has been
     * merged in.
     */
    @Getter private final HashMap<Integer, ServerObjectEntry> mNetworkObjects = new HashMap<>();

    /** Stores per-owner singletons. Can be looked up with getIdSingletons */
    private final HashMap<Integer, SingletonStore> mIdSingletons = new HashMap<>();

    /**
     * Constructor for {@link ServerNetworkManager}.
     *
     * @param manager back reference to {@link NetworkManager}
     * @param port target port to listen on
     * @param clientLoadedEvent callback for client connections
     * @param gameStartEventHandler callback for when the game starts
     */
    public ServerNetworkManager(
            NetworkManager manager,
            int port,
            NetworkManager.IClientLoadedEvent clientLoadedEvent,
            NetworkManager.IGameStartEvent gameStartEventHandler,
            NetworkManager.IGameEndEvent gameEndEventHandler)
            throws IOException {
        mManager = manager;
        mServer = new Server(port, mListener);
        mClientLoadedEvent = clientLoadedEvent;
        mGameStartEventHandler = gameStartEventHandler;
        mGameEndEventHandler = gameEndEventHandler;
    }

    public void start() {
        if (mGameState == ServerGameState.LOBBY) {
            mGameState = ServerGameState.STARTING;
        }
    }

    /** Start the game, load game scene. */
    void startGame() {
        Engine engine = Engine.getInstance();

        mManager.createGameScene(true);

        if (engine.getPresentationScene() == Scene.getActiveScene()) {
            engine.loadPresentationScene(mManager.getGameScene());
        } else {
            engine.activateScene(mManager.getGameScene());
        }

        if (mGameStartEventHandler != null) {
            mGameStartEventHandler.handle(mManager);
        }

        for (ServerClient c : mServer.getClients()) {
            try {
                c.sendBytes(new byte[] {NetworkConfig.Codes.MESSAGE_HOST_STARTED});
            } catch (IOException e) {
                e.printStackTrace();
                c.closeSocket();
            }
        }
    }

    /**
     * Spawns a network object on server, if linked to a game it will also spawn it on the game.
     *
     * @param owner target owner of the object
     * @param templateId ID of spawnable template
     * @return reference to the newly spawned network object
     */
    public Reference<NetworkObject> spawnNetworkObject(ServerClient owner, int templateId) {
        return spawnNetworkObject(owner.getNetworkID(), templateId);
    }

    /**
     * Spawns a network object on server by ID.
     *
     * @param ownerId target owner of the object. For server (AI) owned objects, use negative IDs
     * @param templateId ID of the spawnable template
     * @return reference to newly spawned network object. {@code null} if invalid template was
     *     passed.
     */
    public Reference<NetworkObject> spawnNetworkObject(int ownerId, int templateId) {
        int netId = this.allocateId();

        NetworkObject networkObject = new NetworkObject(netId, ownerId, true, mManager);
        GameObject object = mManager.getSpawnableTemplates().instantiate(templateId);

        if (object == null) {
            log.warning("Failed to instantiate template ID " + templateId);
            return null;
        }

        object.addComponent(networkObject);
        Reference<NetworkObject> ref = networkObject.getReference(NetworkObject.class);
        object.getTransform()
                .setLocal3DTransformation(new Vector3f(), new Quaternionf(), new Vector3f(1));

        mManager.getGameScene().addRootObject(object);

        this.mNetworkObjects.put(netId, new ServerObjectEntry(ref, templateId));
        networkObject.networkInitialize();

        return ref;
    }

    /**
     * Send an event to the clients.
     *
     * @param event event we send
     * @param stream serialized data of the event
     */
    public void sendEvent(ServerEvent<?> event, ByteArrayOutputStream stream) throws IOException {

        if (mServer == null) return;

        byte[] msg = stream.toByteArray();

        int oid = event.getNetworkObject().getOwnerId();

        EventRecipients recipients = event.getRecipients();

        if (recipients == EventRecipients.OWNER) {
            if (oid < 0) {
                ByteArrayInputStream bis = new ByteArrayInputStream(msg);
                DataInputStream dis = new DataInputStream(bis);
                dis.readByte(); // messageId
                dis.readInt(); // networkObjectId
                dis.readInt(); // eventId
                event.handle(dis);
            } else {
                ServerClient c = mServer.getClient(oid);
                if (c != null) {
                    c.sendBytes(msg);
                }
            }
        } else { // Both ACTIVE_CLIENTS and ALL_CLIENTS for now
            for (ServerClient c : mServer.getClients()) {
                c.sendBytes(msg);
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(msg);
            DataInputStream dis = new DataInputStream(bis);
            dis.readByte(); // messageId
            dis.readInt(); // networkObjectId
            dis.readInt(); // eventId
            event.handle(dis);
        }
    }

    /** Destroy the server, and tell {@link NetworkManager} about it. */
    public void destroy() {
        if (mServer == null) return;

        if (mGameEndEventHandler != null) {
            mGameEndEventHandler.handle(mManager);
        }

        mServer.dispose();
        mServer = null;

        mNetworkObjects.values().stream()
                .map(e -> e.mNetworkObject)
                .filter(Reference::isValid)
                .map(Reference::get)
                .map(NetworkObject::getGameObject)
                .forEach(GameObject::destroy);
    }

    /**
     * Get collection of clients connected to the server.
     *
     * @return collection of clients connected to the server. {@code null} if the server has been
     *     destroyed.
     */
    public Collection<ServerClient> getClients() {

        if (mServer == null) {
            return null;
        }

        return mServer.getClients();
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

    /** Network update, called by {@link NetworkManager}. */
    void networkUpdate() {
        if (mServer == null) {
            return;
        }

        switch (mGameState) {
            case STARTING:
                startGame();
                mGameState = ServerGameState.IN_PROGRESS;
                break;
            default:
                break;
        }

        if (mGameState == ServerGameState.STARTING) return;

        try (SceneOverride __ = new SceneOverride(mManager.getGameScene())) {
            mServer.updateClientList();
            mServer.processClientRequests(NetworkConfig.MAX_CLIENT_REQUESTS);

            mNetworkObjects
                    .entrySet()
                    .removeIf(
                            entry -> {
                                NetworkObject obj = entry.getValue().mNetworkObject.get();
                                if (obj != null) {
                                    obj.beforeNetSerialize();
                                    return false;
                                }
                                return true;
                            });

            clientUpdate();
        }
    }

    /** Late network update, called by {@link NetworkManager}. */
    void lateNetworkUpdate() {

        if (mServer == null) {
            Engine engine = Engine.getInstance();

            if (engine.getPresentationScene() == mManager.getGameScene()) {
                engine.loadPresentationScene(Scene.getActiveScene());
                engine.unloadScene(mManager.getGameScene());
            }

            mManager.onServerDestroy();
        } else {
            try (SceneOverride __ = new SceneOverride(mManager.getGameScene())) {
                for (ServerClient c : mServer.getClients()) {
                    if (!c.isInGame()) {
                        continue;
                    }
                    for (ServerObjectEntry entry : mNetworkObjects.values()) {
                        entry.updateClient(c);
                    }
                }
                mNetworkObjects
                        .entrySet()
                        .removeIf(
                                entry -> {
                                    NetworkObject obj = entry.getValue().mNetworkObject.get();
                                    if (obj != null) {
                                        obj.resetUpdateMask();
                                        return false;
                                    }
                                    return true;
                                });
            }
        }
    }

    /**
     * Allocates an id for an object.
     *
     * @return the allocated id.
     */
    private int allocateId() {
        return mNetworkObjectCounter.getAndIncrement();
    }

    /** Sends updated server state to the clients. */
    private void clientUpdate() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(NetworkConfig.Codes.MESSAGE_UPDATE_STATE);
            stream.writeFloat(Engine.getInstance().getCurTime());

            stream.flush();
            stream.close();
            bos.flush();
            bos.close();

            byte[] msg = bos.toByteArray();

            for (ServerClient c : mServer.getClients()) {
                try {
                    c.sendBytes(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    c.closeSocket();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
