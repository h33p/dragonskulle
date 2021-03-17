/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.IClientListener;
import org.dragonskulle.network.NetworkClient;
import org.dragonskulle.network.Templates;

/**
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class ClientNetworkManager extends NetworkManager {
    public static interface IConnectionResultHandler {
        void handle(boolean success);
    }

    private static enum ConnectionState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        JOINED_GAME,
        CONNECTION_ERROR,
        CLEAN_DISCONNECTED
    }

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
        public void receivedInput(String msg) {}

        @Override
        public void receivedBytes(byte[] bytes) {}

        @Override
        public void serverClosed() {
            mNextConnectionState.set(ConnectionState.CLEAN_DISCONNECTED);
        }

        @Override
        public void disconnected() {
            mNextConnectionState.set(ConnectionState.CONNECTION_ERROR);
        }

        @Override
        public void connectedToServer() {
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
            Reference<NetworkObject> networkObjectToUpdate = getNetworkObject(idToUpdate);
            if (networkObjectToUpdate == null) {
                log.info("Should have spawned! Couldn't find nob id :" + idToUpdate);
                return;
            }
            try {
                networkObjectToUpdate.get().updateFromBytes(payload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void spawnNetworkObject(byte[] payload) {
            int objectId = NetworkObject.getIntFromBytes(payload, SPAWN_OBJECT_ID);
            int spawnTemplateId = NetworkObject.getIntFromBytes(payload, SPAWN_TEMPLATE_ID);
            spawnNewNetworkObject(objectId, spawnTemplateId);
        }
    }

    private static final int SPAWN_OBJECT_ID = 0;
    private static final int SPAWN_TEMPLATE_ID = SPAWN_OBJECT_ID + 4;

    private NetworkClient mClient;
    private IClientListener mListener = new Listener();
    @Getter private ConnectionState mConnectionState = ConnectionState.NOT_CONNECTED;
    private AtomicReference<ConnectionState> mNextConnectionState = new AtomicReference<>(null);
    private Scene mGameScene;
    private Scene mPrevScene;
    private IConnectionResultHandler mConnectionHandler;
	private int mTicksWithoutRequests = 0;

    /** An map of references to objects. */
    private final HashMap<Integer, Reference<NetworkObject>> mNetworkObjectReferences =
            new HashMap<>();

    public ClientNetworkManager(Scene gameScene) {
        mGameScene = gameScene;
    }

    @Override
    public boolean isServer() {
        return false;
    }

    /** Connect to a server */
    public void connect(String ip, int port, IConnectionResultHandler handler) {
        if (mClient != null && mConnectionState == ConnectionState.NOT_CONNECTED) {
            mPrevScene = null;
            onDisconnect();
        }

        if (mClient == null) {
            mConnectionState = ConnectionState.CONNECTING;
            mClient = new NetworkClient(ip, port, mListener);
            mConnectionHandler = handler;
        }
    }

    public void sendToServer(byte[] message) {
        mClient.sendBytes(message);
    }

    @Override
    protected void networkUpdate() {

        ConnectionState nextState = mNextConnectionState.getAndSet(null);

        if (nextState != null) {

            System.out.println(nextState.toString());
            System.out.println(mConnectionState.toString());

            if (mConnectionState == ConnectionState.CONNECTING) {
                switch (nextState) {
                    case CONNECTED:
                        joinGame();
                        if (mConnectionHandler != null) mConnectionHandler.handle(true);
                        break;
                    case CONNECTION_ERROR:
                        mClient.dispose();
                        mClient = null;
                        if (mConnectionHandler != null) mConnectionHandler.handle(false);
                        mConnectionState = ConnectionState.NOT_CONNECTED;
                        break;
                    default:
                        break;
                }
            } else if (mConnectionState == ConnectionState.JOINED_GAME) {
                // TODO: handle lobby -> game transition here
				onDisconnect();
            }
        }

        if (mConnectionState == ConnectionState.JOINED_GAME) {
			if (mClient.processRequests() <= 0) {
				mTicksWithoutRequests++;
				if (mTicksWithoutRequests > 320)
					onDisconnect();
				else if (mTicksWithoutRequests == 100)
					log.info("100 ticks without updates! 220 more till disconnect!");
			} else
				mTicksWithoutRequests = 0;
        }
    }

    @Override
    protected void joinLobby() {}

    @Override
    protected void joinGame() {
        Engine engine = Engine.getInstance();

        if (mPrevScene == null) mPrevScene = Scene.getActiveScene();

		Scene.getActiveScene().moveRootObjectToScene(getGameObject(), mGameScene);

        if (engine.getPresentationScene() == Scene.getActiveScene())
            engine.loadPresentationScene(mGameScene);
        else engine.activateScene(mGameScene);

        mConnectionState = ConnectionState.JOINED_GAME;
    }

    @Override
    protected void onDestroy() {
        mPrevScene = null;
        onDisconnect();
    }

    protected void onDisconnect() {
        Engine engine = Engine.getInstance();

        if (mPrevScene != null) {
			Scene.getActiveScene().moveRootObjectToScene(getGameObject(), mPrevScene);

            if (engine.getPresentationScene() == Scene.getActiveScene())
                engine.loadPresentationScene(mPrevScene);
            else engine.activateScene(mPrevScene);
        }

        mConnectionState = ConnectionState.NOT_CONNECTED;
		if (mClient != null) {
			mClient.dispose();
			mClient = null;
		}
    }

    /**
     * Gets a network object by id.
     *
     * @param networkObjectId the id of the object
     * @return the network object found, if none exists then null.
     */
    public Reference<NetworkObject> getNetworkObject(int networkObjectId) {
        log.fine(mNetworkObjectReferences.toString());
        return mNetworkObjectReferences.get(networkObjectId);
    }

    private Reference<NetworkObject> spawnNewNetworkObject(int networkObjectId, int templateId) {
        // TODO: use member templates
        final GameObject go = Templates.instantiate(templateId);
        final NetworkObject nob = new NetworkObject(networkObjectId, false);
        go.addComponent(nob);
        Reference<NetworkObject> ref = nob.getReference(NetworkObject.class);
        log.info("adding a new root object to the scene");
        log.info("nob to be spawned is : " + nob.toString());
        mGameScene.addRootObject(go);
        this.mNetworkObjectReferences.put(nob.getId(), ref);

        return ref;
    }
}
