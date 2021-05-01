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

    public static final float TIMEOUT = 1;

    private static final ReentrantLock sEngineLock = new ReentrantLock();

    public NetworkedTestContext(
            int numClients,
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectedClientEvent onConnectedOnServer,
            IGameStartEvent onGameStart,
            IConnectionResultEvent onClientConnected) {
        mServer = setupServer(templates, sceneBuilder, onConnectedOnServer, onGameStart);

        for (int i = 0; i < numClients; i++) {
            mClients.add(setupClient(templates, sceneBuilder, onClientConnected, mServer));
        }
    }

    public NetworkedTestContext(
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectedClientEvent onConnectedOnServer,
            IGameStartEvent onGameStart,
            IConnectionResultEvent onClientConnected) {
        this(1, templates, sceneBuilder, onConnectedOnServer, onGameStart, onClientConnected);
    }

    public NetworkedSceneContext getClient(int i) {
        return mClients.get(i);
    }

    public NetworkedSceneContext getClient() {
        return mClients.get(0);
    }

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

    public ServerNetworkManager getServerManager() {
        return mServer.getManager().getServerManager();
    }

    public Scene getServerScene(int i) {
        return mServer.getManager().getGameScene();
    }

    public ClientNetworkManager getClientManager(int i) {
        return getClient(i).getManager().getClientManager();
    }

    public Scene getClientScene(int i) {
        return getClient(i).getManager().getGameScene();
    }

    public Scene getClientScene() {
        return getClientScene(0);
    }

    public ClientNetworkManager getClientManager() {
        return getClientManager(0);
    }

    public <T extends NetworkableComponent> Reference<T> getServerComponent(Class<T> type) {
        return getNetworkComponent(mServer.getManager(), type);
    }

    public <T extends NetworkableComponent> Reference<T> getClientComponent(int i, Class<T> type) {
        return getNetworkComponent(getClient(i).getManager(), type);
    }

    public <T extends NetworkableComponent> Reference<T> getClientComponent(Class<T> type) {
        return getClientComponent(0, type);
    }

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

    private static int PORT = 7000;

    private static NetworkedSceneContext setupServer(
            TemplateManager templates,
            ISceneBuilder sceneBuilder,
            IConnectedClientEvent onConnectedOnServer,
            IGameStartEvent onGameStart) {

        NetworkedSceneContext ctx = new NetworkedSceneContext(templates, sceneBuilder);

        ctx.mFuture =
                ctx.mFuture
                        .then(
                                (__) ->
                                        ctx.getManager()
                                                .createServer(
                                                        PORT, onConnectedOnServer, onGameStart))
                        .then(
                                (__) -> {
                                    assert (ctx.getManager().getServerManager() != null);
                                });

        return ctx;
    }

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
                                                PORT,
                                                (gameScene, manager, netID) -> {
                                                    if (onClientConnected != null) {
                                                        onClientConnected.handle(
                                                                gameScene, manager, netID);
                                                    }
                                                    log.info("CONNECTED CLIENT");
                                                    assert (netID >= 0);
                                                    called[0] = true;
                                                }))
                .awaitTimeout(TIMEOUT, (__) -> called[0]);

        return ctx;
    }
}
