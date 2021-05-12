/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.core.futures.Future;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkManager.IClientLoadedEvent;
import org.dragonskulle.network.components.NetworkManager.IConnectedClientEvent;
import org.dragonskulle.network.components.NetworkManager.IConnectionResultEvent;
import org.dragonskulle.network.components.NetworkManager.IGameStartEvent;
import org.dragonskulle.network.components.NetworkManager.ISceneBuilder;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.ServerNetworkManager;

/**
 * Provides a test context for networked tests.
 *
 * @author Aurimas Blažulionis
 *     <p>This context stores a number {@link NetworkedSceneContext} for server, and clients, where
 *     every client will be connected to the same server.
 */
@Getter
@Log
@Accessors(prefix = "m")
public class NetworkedTestContext {
    private final NetworkedSceneContext mServer;
    private final List<NetworkedSceneContext> mClients = new ArrayList<>();

    /** Public timeout for timed waits. */
    public static final float TIMEOUT = 5;
    /** Port used for tests. */
    private static final int sPORT = 7000;

    private static final ReentrantLock sEngineLock = new ReentrantLock();

    /**
     * Constructor for {@link NetworkedTestContext}.
     *
     * @param numClients number of clients to create.
     * @param templates networked templates to use.
     * @param sceneBuilder game scene builder.
     * @param onConnectedOnServer callback to be called when a client connects to server.
     * @param onLoadedOnServer callback to be called when a client informs the server about loading
     *     into the game.
     * @param onGameStart callback to be called on server when the game is fully loaded.
     * @param onClientConnected callback to be called on clients when the client fully connects.
     */
    public NetworkedTestContext(
            int numClients,
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectedClientEvent onConnectedOnServer,
            IClientLoadedEvent onLoadedOnServer,
            IGameStartEvent onGameStart,
            IConnectionResultEvent onClientConnected) {
        mServer =
                setupServer(
                        templates,
                        sceneBuilder,
                        onConnectedOnServer,
                        onLoadedOnServer,
                        onGameStart);

        for (int i = 0; i < numClients; i++) {
            mClients.add(setupClient(templates, sceneBuilder, onClientConnected, mServer));
        }
    }

    /**
     * Constructor for {@link NetworkedTestContext}.
     *
     * <p>This will create a context with 1 client.
     *
     * @param templates networked templates to use.
     * @param sceneBuilder game scene builder.
     * @param onConnectedOnServer callback to be called when a client connects to server.
     * @param onLoadedOnServer callback to be called when a client informs the server about loading
     *     into the game.
     * @param onGameStart callback to be called on server when the game is fully loaded.
     * @param onClientConnected callback to be called on clients when the client fully connects.
     */
    public NetworkedTestContext(
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectedClientEvent onConnectedOnServer,
            IClientLoadedEvent onLoadedOnServer,
            IGameStartEvent onGameStart,
            IConnectionResultEvent onClientConnected) {
        this(
                1,
                templates,
                sceneBuilder,
                onConnectedOnServer,
                onLoadedOnServer,
                onGameStart,
                onClientConnected);
    }

    /**
     * Get a client context by index.
     *
     * @param i index of the client.
     * @return {@link NetworkedSceneContext} for the particular client.
     */
    public NetworkedSceneContext getClient(int i) {
        return mClients.get(i);
    }

    /**
     * Get the first client's context.
     *
     * @return {@link NetworkedSceneContext} for the first client.
     */
    public NetworkedSceneContext getClient() {
        return mClients.get(0);
    }

    /** Execute all futures in the context, and close the system. */
    public void execute() {
        for (NetworkedSceneContext client : mClients) {
            client.syncWith(mServer).then((__) -> client.getManager().closeInstance());
        }

        for (NetworkedSceneContext client : mClients) {
            mServer.syncWith(client);
        }

        mServer.then((__) -> mServer.getManager().closeInstance());

        for (NetworkedSceneContext client : mClients) {
            client.awaitTimeout(TIMEOUT, (__) -> !client.getManager().isClient());
            mServer.syncWith(client);
        }

        mServer.awaitTimeout(TIMEOUT, (__) -> !mServer.getManager().isServer());

        sEngineLock.lock();

        try {
            Future[] futures = new Future[1 + mClients.size()];

            futures[0] = mServer.mFuture;

            int cnt = 0;

            for (NetworkedSceneContext client : mClients) {
                futures[++cnt] = client.mFuture;
            }

            Engine.getInstance().startWithFutures(futures);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            for (NetworkedSceneContext client : mClients) {
                client.getManager().closeInstance();
            }
            mServer.getManager().closeInstance();
            sEngineLock.unlock();
        }
    }

    /**
     * Get a server manager.
     *
     * @return server's server manager.
     */
    public ServerNetworkManager getServerManager() {
        return mServer.getManager().getServerManager();
    }

    /**
     * Get server's game scene.
     *
     * @return server's game scene.
     */
    public Scene getServerScene() {
        return mServer.getManager().getGameScene();
    }

    /**
     * Get client's scene.
     *
     * @param i client's index.
     * @return client's game scene.
     */
    public Scene getClientScene(int i) {
        return getClient(i).getManager().getGameScene();
    }

    /**
     * Get first client's scene.
     *
     * @return first client's game scene.
     */
    public Scene getClientScene() {
        return getClientScene(0);
    }

    /**
     * Get a client manager.
     *
     * @param i client's index.
     * @return client's client manager.
     */
    public ClientNetworkManager getClientManager(int i) {
        return getClient(i).getManager().getClientManager();
    }

    /**
     * Get first client's manager.
     *
     * @return first client's client manager.
     */
    public ClientNetworkManager getClientManager() {
        return getClientManager(0);
    }

    /**
     * Get a server component.
     *
     * @param <T> type of the input type class.
     * @param type component's type.
     * @return reference to the component, if one exists. {@code null} otherwise.
     */
    public <T extends NetworkableComponent> Reference<T> getServerComponent(Class<T> type) {
        return getNetworkComponent(mServer.getManager(), type);
    }

    /**
     * Get a client component.
     *
     * @param <T> type of the input type class.
     * @param i client's index.
     * @param type component's type.
     * @return reference to the component, if one exists. {@code null} otherwise.
     */
    public <T extends NetworkableComponent> Reference<T> getClientComponent(int i, Class<T> type) {
        return getNetworkComponent(getClient(i).getManager(), type);
    }

    /**
     * Get a client component on the first client.
     *
     * @param <T> type of the input type class.
     * @param type component's type.
     * @return reference to the component, if one exists. {@code null} otherwise.
     */
    public <T extends NetworkableComponent> Reference<T> getClientComponent(Class<T> type) {
        return getClientComponent(0, type);
    }

    /**
     * Get a network component.
     *
     * @param <T> type of the input type class.
     * @param manager network manager to get the component from.
     * @param type type of the component.
     * @return reference to the component. {@code null} if does not exist.
     */
    private static <T extends NetworkableComponent> Reference<T> getNetworkComponent(
            NetworkManager manager, Class<T> type) {

        Stream<NetworkObject> objs = manager.getNetworkObjects();

        if (objs == null) {
            return null;
        }

        return objs.map(c -> c.getGameObject().getComponent(type))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Setup a server's {@link NetworkedSceneContext}.
     *
     * @param templates networked templates to use.
     * @param sceneBuilder game scene builder.
     * @param onConnectedOnServer callback to be called when a client connects to server.
     * @param onLoadedOnServer callback to be called when a client informs the server about loading
     *     into the game.
     * @param onGameStart callback to be called on server when the game is fully loaded.
     * @return server's networked scene context.
     */
    private static NetworkedSceneContext setupServer(
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectedClientEvent onConnectedOnServer,
            IClientLoadedEvent onLoadedOnServer,
            IGameStartEvent onGameStart) {

        NetworkedSceneContext ctx = new NetworkedSceneContext(templates, sceneBuilder);

        ctx.mFuture =
                ctx.mFuture
                        .then(
                                (__) ->
                                        ctx.getManager()
                                                .createServer(
                                                        sPORT,
                                                        null,
                                                        onConnectedOnServer,
                                                        onLoadedOnServer,
                                                        onGameStart,
                                                        null))
                        .then(
                                (__) -> {
                                    assert (ctx.getManager().getServerManager() != null);
                                });

        return ctx;
    }

    /**
     * Setup a client's {@link NetworkedSceneContext}.
     *
     * @param templates networked templates to use.
     * @param sceneBuilder game scene builder.
     * @param onClientConnected callback to be called on clients when the client fully connects.
     * @param serverSync context to synchronize against.
     * @return server's networked scene context.
     */
    private static NetworkedSceneContext setupClient(
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectionResultEvent onClientConnected,
            NetworkedSceneContext serverSync) {
        NetworkedSceneContext ctx = new NetworkedSceneContext(templates, sceneBuilder);

        boolean[] called = {false};

        ctx.syncWith(serverSync)
                .then(
                        (scene) ->
                                ctx.getManager()
                                        .createClient(
                                                "127.0.0.1",
                                                sPORT,
                                                (manager, netID) -> {
                                                    if (onClientConnected != null) {
                                                        onClientConnected.handle(manager, netID);
                                                    }
                                                    log.fine("CONNECTED CLIENT");
                                                    assert (netID >= 0);
                                                    called[0] = true;
                                                },
                                                null,
                                                null))
                .awaitTimeout(TIMEOUT, (__) -> called[0]);

        return ctx;
    }
}
