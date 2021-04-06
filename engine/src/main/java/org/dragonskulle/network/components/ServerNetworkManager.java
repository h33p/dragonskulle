/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.IServerListener;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.Server;
import org.dragonskulle.network.ServerClient;

/**
 * Server network manager
 *
 * @author Aurimas Blažulionis
 *     <p>This class is composed in NetworkManager, and handles all things server.
 */
@Accessors(prefix = "m")
@Log
public class ServerNetworkManager {
    /** Server event listener */
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
        public void clientActivated(ServerClient client) {
            if (mConnectedClientHandler != null)
                mConnectedClientHandler.handle(mManager.getGameScene(), mManager, client);
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

            if (!obj.handleClientRequest(requestID, stream))
                log.warning(
                        "Client "
                                + client.getNetworkID()
                                + " passed invalid request: "
                                + requestID
                                + " on object: "
                                + objectID);
        }
    }

    public static class ServerObjectEntry {
        @Getter private final Reference<NetworkObject> mNetworkObject;
        @Getter private final int mTemplateId;
        @Getter private final Set<ServerClient> mSpawnedFor;

        public ServerObjectEntry(Reference<NetworkObject> networkObject, int templateId) {
            mNetworkObject = networkObject;
            mTemplateId = templateId;
            mSpawnedFor = new HashSet<>();
        }

        public void updateClient(ServerClient client) {

            NetworkObject obj = mNetworkObject.get();

            if (obj == null) return;

            boolean forceUpdate = false;

            // Send a spawn message to the client, if haven't already
            if (mSpawnedFor.add(client)) {
                byte[] spawnMessage =
                        NetworkMessage.build(
                                NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT,
                                NetworkMessage.convertIntsToByteArray(
                                        obj.getNetworkObjectId(), obj.getOwnerId(), mTemplateId));
                client.sendBytes(spawnMessage);
                forceUpdate = true;
            }

            obj.sendUpdate(client, forceUpdate);
        }
    }

    /** Server event listener */
    private final Listener mListener = new Listener();
    /** Underlying server instance */
    private final Server mServer;
    /** Back reference to {@link NetworkManager} */
    private final NetworkManager mManager;
    /** Callback for connected clients */
    private final NetworkManager.IConnectedClientEvent mConnectedClientHandler;
    /** The Counter used to assign objects a unique id. */
    private final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);

    /**
     * The Network objects - this can be moved to game instance but no point until game has been
     * merged in.
     */
    @Getter private final HashMap<Integer, ServerObjectEntry> mNetworkObjects = new HashMap<>();

    /**
     * Constructor for {@link ServerNetworkManager}
     *
     * @param manager back reference to {@link NetworkManager}
     * @param port target port to listen on
     * @param connectedClientHandler callback for client connections
     */
    public ServerNetworkManager(
            NetworkManager manager,
            int port,
            NetworkManager.IConnectedClientEvent connectedClientHandler)
            throws IOException {
        mManager = manager;
        mServer = new Server(port, mListener);
        mConnectedClientHandler = connectedClientHandler;
        startGame();
    }

    /** Start the game, load game scene */
    void startGame() {
        Engine engine = Engine.getInstance();

        mManager.createGameScene(true);

        if (engine.getPresentationScene() == Scene.getActiveScene()) {
            engine.loadPresentationScene(mManager.getGameScene());
        } else {
            engine.activateScene(mManager.getGameScene());
        }
    }

    /**
     * Spawns a network object on server, if linked to a game it will also spawn it on the game.
     *
     * @param owner target owner of the object
     * @param templateId ID of spawnable template
     */
    public Reference<NetworkObject> spawnNetworkObject(ServerClient owner, int templateId) {
        return spawnNetworkObject(owner.getNetworkID(), templateId);
    }

    /**
     * Spawns a network object on server by ID.
     *
     * @param ownerId target owner of the object. For server (AI) owned objects, use negative IDs
     * @param templateId ID of the spawnable template
     */
    public Reference<NetworkObject> spawnNetworkObject(int ownerId, int templateId) {
        int netId = this.allocateId();

        NetworkObject networkObject = new NetworkObject(netId, ownerId, true, mManager);
        GameObject object = mManager.getSpawnableTemplates().instantiate(templateId);
        object.addComponent(networkObject);
        Reference<NetworkObject> ref = networkObject.getReference(NetworkObject.class);

        mManager.getGameScene().addRootObject(object);

        this.mNetworkObjects.put(netId, new ServerObjectEntry(ref, templateId));
        networkObject.networkInitialize();

        return ref;
    }

    /** Destroy the server, and tell {@link NetworkManager} about it */
    public void destroy() {
        mServer.dispose();

        mNetworkObjects.values().stream()
                .map(e -> e.mNetworkObject)
                .filter(Reference::isValid)
                .map(Reference::get)
                .map(NetworkObject::getGameObject)
                .forEach(GameObject::destroy);

        mManager.onServerDestroy();
    }

    /** Network update, called by {@link NetworkManager} */
    void networkUpdate() {
        if (mServer == null) return;

        mServer.updateClientList();
        mServer.processClientRequests(NetworkConfig.MAX_CLIENT_REQUESTS);

        for (Entry<Integer, ServerObjectEntry> entry : mNetworkObjects.entrySet()) {
            NetworkObject obj = entry.getValue().mNetworkObject.get();
            if (obj != null) obj.beforeNetSerialize();
            else mNetworkObjects.remove(entry.getKey());
        }

        clientUpdate();

        for (ServerClient c : mServer.getClients()) {
            for (ServerObjectEntry entry : mNetworkObjects.values()) {
                entry.updateClient(c);
            }
        }

        for (Entry<Integer, ServerObjectEntry> entry : mNetworkObjects.entrySet()) {
            NetworkObject obj = entry.getValue().mNetworkObject.get();
            if (obj != null) obj.resetUpdateMask();
            else mNetworkObjects.remove(entry.getKey());
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

    /** Sends updated server state to the clients */
    private void clientUpdate() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeFloat(Engine.getInstance().getCurTime());

            stream.flush();
            stream.close();
            bos.flush();
            bos.close();

            byte[] msg =
                    NetworkMessage.build(
                            NetworkConfig.Codes.MESSAGE_UPDATE_STATE, bos.toByteArray());

            for (ServerClient c : mServer.getClients()) c.sendBytes(msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
