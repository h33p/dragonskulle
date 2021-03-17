/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.DataInputStream;
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
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.Server;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.ServerListener;
import org.dragonskulle.network.Templates;

/** @author Aurimas Blažulionis */
@Accessors(prefix = "m")
@Log
public class ServerNetworkManager extends NetworkManager {
    public class Listener implements ServerListener {
        /**
         * Client connected event.
         *
         * @param client the client
         * @param out the out
         * @return which network ID was allocated for the client
         */
        @Override
        public int clientConnected(ServerClient client) {
            return mServer.addConnectedClient(client);
        }

        @Override
        public void clientActivated(ServerClient client) {
            spawnNetworkObject(client, Templates.find("cube"));
            spawnNetworkObject(client, Templates.find("capital"));
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

    private static class ServerObjectEntry {
        private final Reference<NetworkObject> mNetworkObject;
        private final int mTemplateId;
        private final Set<ServerClient> mSpawnedFor;

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

    private Listener mListener = new Listener();
    private Server mServer;

    /** The Counter used to assign objects a unique id. */
    private final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);

    /**
     * The Network objects - this can be moved to game instance but no point until game has been
     * merged in.
     */
    @Getter private final HashMap<Integer, ServerObjectEntry> mNetworkObjects = new HashMap<>();

    private Scene mGameScene;
    private Scene mPrevScene;

    public ServerNetworkManager(Scene gameScene) {
        mGameScene = gameScene;
    }

    @Override
    public boolean isServer() {
        return true;
    }

    public void startGame() {
        try {
            mServer = new Server(7000, mListener);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Engine engine = Engine.getInstance();

        if (mPrevScene == null) mPrevScene = Scene.getActiveScene();

        Scene.getActiveScene().moveRootObjectToScene(getGameObject(), mGameScene);

        if (engine.getPresentationScene() == Scene.getActiveScene())
            engine.loadPresentationScene(mGameScene);
        else engine.activateScene(mGameScene);
    }

    /**
     * Spawns a network object on server, if linked to a game it will also spawn it on the game.
     *
     * @param owner target owner of the object
     * @param templateId ID of spawnable template
     */
    private Reference<NetworkObject> spawnNetworkObject(ServerClient owner, int templateId) {
        int netId = this.allocateId();

        NetworkObject networkObject = new NetworkObject(netId, owner.getNetworkID(), true);
        GameObject object = Templates.instantiate(templateId);
        object.addComponent(networkObject);
        Reference<NetworkObject> ref = networkObject.getReference(NetworkObject.class);

        mGameScene.addRootObject(object);

        this.mNetworkObjects.put(netId, new ServerObjectEntry(ref, templateId));

        return ref;
    }

    /**
     * Gets a network object.
     *
     * @param networkObjectId the id of the object
     * @return the network object found, null if not found
     */
    private ServerObjectEntry getNetworkObject(int networkObjectId) {
        return this.mNetworkObjects.get(networkObjectId);
    }

    @Override
    public void networkUpdate() {
        if (mServer == null) return;

        mServer.updateClientList();
        mServer.processClientRequests(16);

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

    protected void joinLobby() {}

    protected void joinGame() {}

    /**
     * Allocates an id for an object.
     *
     * @return the allocated id.
     */
    private int allocateId() {
        return mNetworkObjectCounter.getAndIncrement();
    }

    @Override
    protected void onDestroy() {
        if (mServer != null) mServer.dispose();
    }
}
