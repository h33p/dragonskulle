/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.core.futures.AwaitFuture.IAwaitFuture;
import org.dragonskulle.core.futures.Future;
import org.dragonskulle.core.futures.ThenFuture;
import org.dragonskulle.core.futures.ThenFuture.IThenFuture;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.network.components.ClientNetworkManager.ConnectionState;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.junit.Test;

/** @author Oscar L, Aurimas Blažulionis */
@Log
public class ServerTest {
    private static final long TIMEOUT = 1;

    private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();
    private static final Scene CLIENT_NETMAN_SCENE = new Scene("client_netman_test");
    private static final NetworkManager CLIENT_NETWORK_MANAGER =
            new NetworkManager(TEMPLATE_MANAGER, (__1, __2) -> new Scene("client_net_test"));

    private static final Scene SERVER_NETMAN_SCENE = new Scene("server_netman_test");
    private static final NetworkManager SERVER_NETWORK_MANAGER =
            new NetworkManager(TEMPLATE_MANAGER, (__1, __2) -> new Scene("server_net_test"));

    private static ReentrantLock sEngineLock = new ReentrantLock();

    static {
        TEMPLATE_MANAGER.addAllObjects(
                new GameObject(
                        "cube",
                        (handle) -> {
                            handle.addComponent(new NetworkedTransform());
                        }),
                new GameObject(
                        "capital",
                        (handle) -> {
                            handle.addComponent(new TestCapitalBuilding());
                        }));

        CLIENT_NETMAN_SCENE.addRootObject(
                new GameObject("netman", handle -> handle.addComponent(CLIENT_NETWORK_MANAGER)));
        SERVER_NETMAN_SCENE.addRootObject(
                new GameObject("netman", handle -> handle.addComponent(SERVER_NETWORK_MANAGER)));
    }

    private static class SceneContext {
        NetworkManager mManager;
        Future mFuture;

        public SceneContext(String name) {
            mManager = new NetworkManager(TEMPLATE_MANAGER, (__1, __2) -> new Scene(name));

            mFuture =
                    new ThenFuture(
                            (scene) -> {
                                scene.addRootObject(
                                        new GameObject(
                                                "netman", handle -> handle.addComponent(mManager)));
                            });
        }

        public SceneContext then(IThenFuture doFn) {
            mFuture = mFuture.then(doFn);
            return this;
        }

        public SceneContext awaitUntil(IAwaitFuture awaitFn) {
            mFuture = mFuture.awaitUntil(awaitFn);
            return this;
        }

        public SceneContext awaitTimeout(float maxSeconds, IAwaitFuture awaitFn) {
            mFuture = mFuture.awaitTimeout(maxSeconds, awaitFn);
            return this;
        }

        public SceneContext syncWith(Future future) {
            mFuture = mFuture.syncWith(future);
            return this;
        }

        public SceneContext syncWith(SceneContext ctx) {
            return syncWith(ctx.mFuture);
        }
    }

    private static class NetworkTestContext {
        final SceneContext mServer;
        final SceneContext mClient;

        public NetworkTestContext() {
            mServer = setupServer();
            mClient = setupClient(mServer);
        }

        public void execute() {

            mClient.syncWith(mServer).then((__) -> mClient.mManager.closeInstance());
            mServer.syncWith(mClient).then((__) -> mServer.mManager.closeInstance());

            mClient.awaitTimeout(TIMEOUT, (__) -> !mClient.mManager.isClient());
            mServer.awaitTimeout(TIMEOUT, (__) -> !mServer.mManager.isServer());

            sEngineLock.lock();

            try {
                Engine.getInstance().startWithFutures(mServer.mFuture, mClient.mFuture);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            } finally {
                mClient.mManager.closeInstance();
                mServer.mManager.closeInstance();
                sEngineLock.unlock();
            }
        }

        public ServerNetworkManager getServerManager() {
            return mServer.mManager.getServerManager();
        }

        public ClientNetworkManager getClientManager() {
            return mClient.mManager.getClientManager();
        }

        public <T extends NetworkableComponent> Reference<T> getServerComponent(Class<T> type) {
            return getNetworkComponent(mServer.mManager, type);
        }

        public <T extends NetworkableComponent> Reference<T> getClientComponent(Class<T> type) {
            return getNetworkComponent(mClient.mManager, type);
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
    }

    private static int PORT = 7000;

    public static SceneContext setupServer() {

        SceneContext ctx = new SceneContext("server_net_test");

        ctx.mFuture =
                ctx.mFuture
                        .then(
                                (scene) ->
                                        ctx.mManager.createServer(
                                                PORT,
                                                (__, man, id) -> {
                                                    log.info("CONNECTED");
                                                    man.getServerManager()
                                                            .spawnNetworkObject(
                                                                    id,
                                                                    TEMPLATE_MANAGER.find("cube"));
                                                    man.getServerManager()
                                                            .spawnNetworkObject(
                                                                    id,
                                                                    TEMPLATE_MANAGER.find(
                                                                            "capital"));
                                                },
                                                null))
                        .then((__) -> assertNotNull(ctx.mManager.getServerManager()));

        return ctx;
    }

    public static SceneContext setupClient(SceneContext serverSync) {
        SceneContext ctx = new SceneContext("server_net_test");

        boolean[] called = {false};

        ctx.syncWith(serverSync)
                .then(
                        (scene) ->
                                ctx.mManager.createClient(
                                        "127.0.0.1",
                                        PORT,
                                        (__1, __2, netid) -> {
                                            log.info("CONNECTED CLIENT");
                                            assertTrue(netid >= 0);
                                            called[0] = true;
                                        }))
                .awaitTimeout(TIMEOUT, (__) -> called[0]);

        return ctx;
    }

    @Test
    public void testConnect() {
        NetworkTestContext ctx = new NetworkTestContext();

        connect(ctx);

        ctx.execute();
    }

    private static void connect(NetworkTestContext ctx) {
        ctx.mServer.awaitTimeout(TIMEOUT, (__) -> !ctx.getServerManager().getClients().isEmpty());
        ctx.mClient.awaitTimeout(
                TIMEOUT,
                (__) -> ctx.getClientManager().getConnectionState() == ConnectionState.JOINED_GAME);
    }

    @Test
    public void testSpawnObject() {
        NetworkTestContext ctx = new NetworkTestContext();

        connect(ctx);
        spawnObject(ctx);

        ctx.execute();
    }

    private static void spawnObject(NetworkTestContext ctx) {
        ctx.mServer.then(
                (__) -> assertTrue(ctx.getServerComponent(TestCapitalBuilding.class) != null));

        ctx.mClient
                .syncWith(ctx.mServer)
                .awaitTimeout(
                        TIMEOUT, (__) -> ctx.getClientComponent(TestCapitalBuilding.class) != null);

        ctx.mClient.then(
                (__) -> {
                    Reference<TestCapitalBuilding> clientCapital =
                            ctx.getClientComponent(TestCapitalBuilding.class);
                    assertNotNull(clientCapital);
                    Reference<TestCapitalBuilding> serverCapital =
                            ctx.getServerComponent(TestCapitalBuilding.class);
                    assertNotNull(serverCapital);

                    int capitalId = clientCapital.get().getNetworkObject().getId();

                    log.info(
                            "\t-----> "
                                    + capitalId
                                    + " "
                                    + serverCapital.get().getNetworkObject().getId());
                    assertEquals(capitalId, serverCapital.get().getNetworkObject().getId());
                    log.info("\t-----> " + capitalId);
                    assert (serverCapital.get().getSyncMe().get() == false);
                    assert (serverCapital.get().getSyncMeAlso().get().equals("Hello World"));
                });

        ctx.mServer.syncWith(ctx.mClient);
    }

    @Test
    public void testModifyCapital() {
        NetworkTestContext ctx = new NetworkTestContext();

        connect(ctx);
        spawnObject(ctx);
        modifyCapital(ctx);

        ctx.execute();
    }

    private void modifyCapital(NetworkTestContext ctx) {
        ctx.mClient.then(
                (__) -> {
                    TestCapitalBuilding component =
                            ctx.getClientComponent(TestCapitalBuilding.class).get();
                    assertFalse(component.getSyncMe().get());
                    assertFalse(component.getSyncMeAlso().get().equals("Goodbye World"));
                });

        ctx.mServer
                .syncWith(ctx.mClient)
                .then(
                        (__) -> {
                            TestCapitalBuilding component =
                                    ctx.getServerComponent(TestCapitalBuilding.class).get();
                            component.setBooleanSyncMe(true);
                            component.setStringSyncMeAlso("Goodbye World");
                        });

        ctx.mClient
                .syncWith(ctx.mServer)
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> {
                            TestCapitalBuilding component =
                                    ctx.getClientComponent(TestCapitalBuilding.class).get();
                            return component.getSyncMe().get()
                                    && component.getSyncMeAlso().get().equals("Goodbye World");
                        });

        ctx.mServer.syncWith(ctx.mClient);
    }

    @Test
    public void testSubmitRequest() {
        NetworkTestContext ctx = new NetworkTestContext();

        connect(ctx);
        spawnObject(ctx);
        submitRequest(ctx);

        ctx.execute();
    }

    private void submitRequest(NetworkTestContext ctx) {
        TestCapitalBuilding[] cap = {null};

        ctx.mClient
                .then(
                        (__) -> {
                            cap[0] = ctx.getClientComponent(TestCapitalBuilding.class).get();
                            cap[0].mPasswordRequest.invoke(
                                    new TestAttackData(TestCapitalBuilding.CORRECT_PASSWORD, 354));
                        })
                .awaitTimeout(TIMEOUT, (__) -> cap[0].getClientToggled().get() == 354);

        ctx.mServer.syncWith(ctx.mClient);
    }

    @Test
    public void testDestroy() {
        NetworkTestContext ctx = new NetworkTestContext();

        connect(ctx);
        spawnObject(ctx);
        destroy(ctx);

        ctx.execute();
    }

    private void destroy(NetworkTestContext ctx) {
        ctx.mServer.then(
                (__) ->
                        ctx.getServerComponent(TestCapitalBuilding.class)
                                .get()
                                .getGameObject()
                                .destroy());
        ctx.mClient
                .syncWith(ctx.mServer)
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                !Reference.isValid(
                                        ctx.getClientComponent(TestCapitalBuilding.class)));

        ctx.mServer.syncWith(ctx.mClient);
    }

    @Test
    public void testSetOwnerId() {
        NetworkTestContext ctx = new NetworkTestContext();

        connect(ctx);
        spawnObject(ctx);
        setOwnerId(ctx);

        ctx.execute();
    }

    private void setOwnerId(NetworkTestContext ctx) {
        int[] ownerId = {-10000};
        TestCapitalBuilding[] serverCap = {null};
        TestCapitalBuilding[] clientCap = {null};

        ctx.mServer
                .then(
                        (__) ->
                                serverCap[0] =
                                        ctx.getServerComponent(TestCapitalBuilding.class).get())
                .then((__) -> ownerId[0] = serverCap[0].getNetworkObject().getOwnerId());

        ctx.mClient
                .syncWith(ctx.mServer)
                .then(
                        (__) ->
                                clientCap[0] =
                                        ctx.getClientComponent(TestCapitalBuilding.class).get())
                .then(
                        (__) -> {
                            assertEquals(ownerId[0], clientCap[0].getNetworkObject().getOwnerId());
                        });

        ctx.mServer
                .syncWith(ctx.mClient)
                .then((__) -> serverCap[0].getNetworkObject().setOwnerId(ownerId[0] + 1));

        ctx.mClient.awaitTimeout(
                TIMEOUT, (__) -> clientCap[0].getNetworkObject().getOwnerId() == ownerId[0] + 1);

        ctx.mServer.syncWith(ctx.mClient);
    }
}
