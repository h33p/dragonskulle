/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.IOException;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateNetworkUpdate;
import org.dragonskulle.components.INetworkUpdate;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.SingletonStore;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.network.ServerClient;

/**
 * Root network manager.
 *
 * @author Aurimas Blažulionis
 * @author Oscar L
 *     <p>Network manager stores internal link to either server or client network managers, which
 *     actually manage internal game state. This manager provides common interface between the two.
 */
@Accessors(prefix = "m")
public class NetworkManager extends Component implements INetworkUpdate, ILateNetworkUpdate {

    /** Simple client connection result handler. */
    public interface IConnectionResultEvent {
        /**
         * Handle the connection result event.
         *
         * @param manager network manager which the event is called from
         * @param netID allocated network ID. If it's negative, connection failed
         */
        void handle(NetworkManager manager, int netID);
    }

    /** Event that gets called when host starts the game. */
    public interface IHostStartedGameEvent {
        /**
         * Handle the host started game event.
         *
         * @param gameScene scene in which the game will be
         * @param manager network manager which the event is called from
         * @param netID allocated network ID.
         */
        void handle(Scene gameScene, NetworkManager manager, int netID);
    }

    /** Event that gets invoked when a client attempts to connect to the server. */
    public interface IConnectionAttemptEvent {
        /**
         * Handle the connection event.
         *
         * <p>Note that this event is invoked on another thread!
         *
         * <p>Note that reconnecting (positive IDs) are not yet supported!
         *
         * @param manager network manager which the event is called from.
         * @param client server client instance.
         * @return {@code null} if the client should be given a new ID. {@code -1} if the client
         *     should be disconnected. A non-negative integer for a specific network ID (in case of
         *     reconnects).
         */
        Integer handle(NetworkManager manager, ServerClient client);
    }

    /** Event that gets invoked when the player disconnects. */
    public interface IHostClosedGameEvent {
        /** Handle the host ended game event. */
        void handle();
    }

    /** Simple server client connection handler interface. */
    public interface IConnectedClientEvent {
        /**
         * Handle client connection on the server.
         *
         * @param gameScene scene in which the game will be run
         * @param manager network manager which the event is called from
         * @param client newly connected network client
         */
        void handle(Scene gameScene, NetworkManager manager, ServerClient client);
    }

    /** Simple server client connection handler interface. */
    public interface IClientLoadedEvent {
        /**
         * Handle client connection on the server.
         *
         * @param gameScene scene in which the game will be run
         * @param manager network manager which the event is called from
         * @param client newly connected network client
         */
        void handle(Scene gameScene, NetworkManager manager, ServerClient client);
    }

    /** Ran on game start. */
    public interface IGameStartEvent {
        /**
         * Handle game start event.
         *
         * @param manager network manager which the event is called from.
         */
        void handle(NetworkManager manager);
    }

    /** Event that gets invoked on the server side whenever the game ends. */
    public interface IGameEndEvent {
        /**
         * Handle game end event.
         *
         * @param manager network manager which the event is called from
         */
        void handle(NetworkManager manager);
    }

    /** Builder interface, used for networked scene building. */
    public interface ISceneBuilder {
        /**
         * Build a scene.
         *
         * @param manager network manager which the event is called from
         * @param isServer if we are building as the server, or the client
         * @return built scene to be used as networked scene
         */
        Scene buildScene(NetworkManager manager, boolean isServer);
    }

    /** Registered spawnable templates. */
    @Getter(AccessLevel.PACKAGE)
    protected final TemplateManager mSpawnableTemplates;
    /** Target game scene. */
    @Getter private Scene mGameScene;

    @Getter(AccessLevel.PACKAGE)
    private final ISceneBuilder mGameSceneBuilder;
    /** Client manager. Exists when there is a client connection */
    @Getter private transient ClientNetworkManager mClientManager;
    /** Server manager. Exists when there is a server instance */
    @Getter private transient ServerNetworkManager mServerManager;

    /**
     * Constructor for network manager.
     *
     * @param templates spawnable templates for objects in the game. Each object has a unique ID,
     *     and it can be looked up by name using findTemplateByName method
     * @param builder builder which will be used to build the game scene.
     */
    public NetworkManager(TemplateManager templates, ISceneBuilder builder) {
        mSpawnableTemplates = templates;
        mGameSceneBuilder = builder;
    }

    @Override
    public void networkUpdate() {
        Scene.getActiveScene().registerSingleton(this);

        if (mGameScene != null) {
            mGameScene.registerSingleton(this);
        }

        if (mServerManager != null) {
            mServerManager.networkUpdate();
        } else if (mClientManager != null) {
            mClientManager.networkUpdate();
        }
    }

    @Override
    public void lateNetworkUpdate() {
        if (mServerManager != null) {
            mServerManager.lateNetworkUpdate();
        } else if (mClientManager != null) {
            mClientManager.lateNetworkUpdate();
        }
    }

    /**
     * Create the game scene.
     *
     * @param isServer {@code true} if called by {@link ServerNetworkManager}
     */
    void createGameScene(boolean isServer) {
        if (mGameScene != null) {
            Engine.getInstance().unloadScene(mGameScene);
        }
        this.mGameScene = mGameSceneBuilder.buildScene(this, isServer);
    }

    /**
     * Create a network client.
     *
     * @param ip IP address to connect to.
     * @param port network port to connect to.
     * @param resultHandler connection result callback.
     * @param startHandler game start event handler.
     * @param closedHandler game end/closed event handler.
     */
    public void createClient(
            String ip,
            int port,
            IConnectionResultEvent resultHandler,
            IHostStartedGameEvent startHandler,
            IHostClosedGameEvent closedHandler) {
        if (mClientManager == null && mServerManager == null) {
            mClientManager =
                    new ClientNetworkManager(
                            this, ip, port, resultHandler, startHandler, closedHandler);
        }
    }

    /**
     * Create a network server.
     *
     * @param port network port to bind.
     * @param connectionAttemptHandler callback that gets called whenever connection is attempted.
     * @param connectionHandler callback that gets called on every client connection.
     * @param loadHandler callback that gets called on every client connection when it loads into
     *     game scene.
     * @param startEventHandler callback that gets called when the game starts.
     * @param endEventHandler callback that gets called when the game ends.
     * @return {@code true} if the server creation was successfull. {@code false} otherwise.
     */
    public boolean createServer(
            int port,
            IConnectionAttemptEvent connectionAttemptHandler,
            IConnectedClientEvent connectionHandler,
            IClientLoadedEvent loadHandler,
            IGameStartEvent startEventHandler,
            IGameEndEvent endEventHandler) {
        if (mClientManager == null && mServerManager == null) {
            try {
                mServerManager =
                        new ServerNetworkManager(
                                this,
                                port,
                                connectionAttemptHandler,
                                connectionHandler,
                                loadHandler,
                                startEventHandler,
                                endEventHandler);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Find template index by name.
     *
     * @param name target name of the template
     * @return {@code null} if not found, integer ID otherwise
     */
    public Integer findTemplateByName(String name) {
        return mSpawnableTemplates.find(name);
    }

    /**
     * Check whether we are running as server.
     *
     * @return {@code true} if we are running as server, {@code false} otherwise
     */
    public boolean isServer() {
        return mServerManager != null;
    }

    /**
     * Check whether we are running as client.
     *
     * @return {@code true} if we are running as client, {@code false} otherwise.
     */
    public boolean isClient() {
        return mClientManager != null;
    }

    /**
     * Get the number of clients in the current network instance.
     *
     * @return Return number of clients in the game.
     */
    public int getClientCount() {
        if (mServerManager != null) {
            return mServerManager.getClients().size();
        }

        if (mClientManager != null) {
            return mClientManager.getPlayerCount();
        }

        return 0;
    }

    /**
     * Gets the server's time.
     *
     * @return server's time. Note that it does not account for latency or anything like that. If
     *     there is no client or server active, {@code -1f} is returned.
     */
    public float getServerTime() {
        if (mServerManager != null) {
            return Engine.getInstance().getCurTime();
        } else if (mClientManager != null) {
            return mClientManager.getServerTime();
        }
        return -1f;
    }

    /**
     * Get a stream of {@link NetworkObject}.
     *
     * @return stream of network objects, or {@code null} if there is no active networked game
     */
    public Stream<NetworkObject> getNetworkObjects() {
        Stream<Reference<NetworkObject>> obj = null;

        if (mServerManager != null) {
            obj = mServerManager.getNetworkObjects();
        } else if (mClientManager != null) {
            obj = mClientManager.getNetworkObjects();
        }

        return obj == null ? null : obj.filter(Reference::isValid).map(Reference::get);
    }

    /**
     * Get stream of objects owned by a particular owner.
     *
     * @param ownerId target owner to yield the objects for
     * @return stream of network objects whose owner is {@code ownerId}. {@code null}, if there is
     *     no networked game active.
     */
    public Stream<NetworkObject> getObjectsOwnedBy(int ownerId) {
        Stream<NetworkObject> obj = getNetworkObjects();

        return obj == null ? null : obj.filter(o -> o.getOwnerId() == ownerId);
    }

    /**
     * Get a network object by its ID.
     *
     * @param netId network ID of an object
     * @return {@link NetworkObject} corresponding to the netId, {@code null} if not found
     */
    public NetworkObject getObjectById(int netId) {
        Stream<NetworkObject> obj = getNetworkObjects();

        return obj == null ? null : obj.filter(o -> o.getId() == netId).findAny().orElse(null);
    }

    /**
     * Get singletons for a object owner.
     *
     * @param ownerId owner of the singletons
     * @return singleton store for the given owner ID. If the store does not exist, a new one gets
     *     created. {@code null} if no client or server is running.
     */
    public SingletonStore getIdSingletons(int ownerId) {
        if (mServerManager != null) {
            return mServerManager.getIdSingletons(ownerId);
        } else if (mClientManager != null) {
            return mClientManager.getIdSingletons(ownerId);
        } else {
            return null;
        }
    }

    /** Close any active client/server instances. */
    public void closeInstance() {
        if (mServerManager != null) {
            mServerManager.destroy();
        }
        if (mClientManager != null) {
            mClientManager.disconnect();
        }
    }

    /** Called whenever client disconnects. */
    void onClientDisconnect() {
        mClientManager = null;
    }

    /** Called whenever server is destroyed. */
    void onServerDestroy() {
        mServerManager = null;
    }

    @Override
    protected void onDestroy() {
        closeInstance();
        if (mServerManager != null) {
            mServerManager.lateNetworkUpdate();
        }
        if (mClientManager != null) {
            mClientManager.lateNetworkUpdate();
        }
        if (mGameScene != null) {
            Engine.getInstance().unloadScene(mGameScene);
        }
        mGameScene = null;
    }
}
