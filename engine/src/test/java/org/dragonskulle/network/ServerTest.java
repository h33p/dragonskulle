/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.java.Log;
import org.dragonskulle.components.lambda.LambdaOnStart;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkableComponent;
import org.junit.Test;
import org.lwjgl.system.NativeResource;

/** @author Oscar L */
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

    private static ReentrantLock sEngineLock = new ReentrantLock();

    private static void cleanupNetmans() {
        log.info("Cleanup netmans");
        if (CLIENT_NETWORK_MANAGER.getClientManager() != null) {
            CLIENT_NETWORK_MANAGER.getClientManager().disconnect();
        }
        if (SERVER_NETWORK_MANAGER.getServerManager() != null) {
            SERVER_NETWORK_MANAGER.getServerManager().destroy();
        }
        log.info("CLEANED UP");
    }

    private static class TestContext implements NativeResource {

        private Thread mTestThread;
        private boolean mShouldExit;
        private int mPort;

        private Throwable mToThrow = null;

        public TestContext(int port) {
            mPort = port;
        }

        /**
         * Network manager destruction does not occur immediately, wait until that stage is reached,
         * and only then recreate netmans.
         */
        private void ensureNetworkIsRecreated() {
            boolean closed = false;

            while (!closed) {
                closed = true;

                sEngineLock.lock();
                if (CLIENT_NETWORK_MANAGER.getClientManager() != null) closed = false;
                if (SERVER_NETWORK_MANAGER.getServerManager() != null) closed = false;
                sEngineLock.unlock();
            }

            sEngineLock.lock();

            SERVER_NETWORK_MANAGER.createServer(
                    mPort,
                    (__, man, id) -> {
                        log.info("CONNECTED");
                        man.getServerManager()
                                .spawnNetworkObject(id, TEMPLATE_MANAGER.find("cube"));
                        man.getServerManager()
                                .spawnNetworkObject(id, TEMPLATE_MANAGER.find("capital"));
                    });

            CLIENT_NETWORK_MANAGER.createClient(
                    "127.0.0.1",
                    mPort,
                    (__1, __2, netid) -> {
                        log.info("CONNECTED CLIENT");
                        assertTrue(netid >= 0);
                    });

            sEngineLock.unlock();
        }

        public synchronized void run(Runnable runnable) throws Throwable {
            mTestThread =
                    new Thread(
                            () -> {
                                try {

                                    ensureNetworkIsRecreated();

                                    runnable.run();
                                } finally {
                                    mShouldExit = true;
                                }
                            });
            sEngineLock.lock();

            cleanupNetmans();

            mTestThread.setUncaughtExceptionHandler(
                    (t, e) -> {
                        mToThrow = e;
                    });
            mTestThread.start();

            Engine.getInstance().loadScene(CLIENT_NETMAN_SCENE, true);
            Engine.getInstance().loadScene(SERVER_NETMAN_SCENE, true);
            Engine.getInstance().startFixedDebug(this::shouldExit);

            try {
                mTestThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (mToThrow != null) {
                throw mToThrow;
            }
        }

        private boolean shouldExit() {

            sEngineLock.unlock();

            if (mShouldExit) {
                return false;
            }

            sEngineLock.lock();

            return true;
        }

        @Override
        public void free() {
            sEngineLock.lock();
            cleanupNetmans();
            mShouldExit = true;
            sEngineLock.unlock();
        }

        public <T extends NetworkableComponent> Reference<T> getServerComponent(Class<T> type) {
            sEngineLock.lock();
            Reference<T> ret =
                    SERVER_NETWORK_MANAGER.getServerManager().getNetworkObjects().values().stream()
                            .map(c -> c.getNetworkObject().get().getGameObject().getComponent(type))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
            sEngineLock.unlock();
            return ret;
        }

        public <T extends NetworkableComponent> Reference<T> getClientComponent(Class<T> type) {
            sEngineLock.lock();
            Reference<T> ret =
                    CLIENT_NETWORK_MANAGER
                            .getClientManager()
                            .getNetworkObjects()
                            .map(c -> c.get().getGameObject().getComponent(type))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
            sEngineLock.unlock();
            return ret;
        }

        private Reference<TestCapitalBuilding> testCapitalSpawnDefaultServer() {
            await().atMost(6, SECONDS)
                    .until(() -> getServerComponent(TestCapitalBuilding.class) != null);
            await().atMost(TIMEOUT * 2, SECONDS)
                    .until(() -> getClientComponent(TestCapitalBuilding.class) != null);

            Reference<TestCapitalBuilding> clientCapital =
                    getClientComponent(TestCapitalBuilding.class);
            assertNotNull(clientCapital);
            Reference<TestCapitalBuilding> serverCapital =
                    getServerComponent(TestCapitalBuilding.class);
            assertNotNull(serverCapital);

            sEngineLock.lock();
            int capitalId = clientCapital.get().getNetworkObject().getId();
            log.info(
                    "\t-----> " + capitalId + " " + serverCapital.get().getNetworkObject().getId());
            assertEquals(capitalId, serverCapital.get().getNetworkObject().getId());
            log.info("\t-----> " + capitalId);
            assert (serverCapital.get().getSyncMe().get() == false);
            assert (serverCapital.get().getSyncMeAlso().get().equals("Hello World"));
            sEngineLock.unlock();
            return serverCapital;
        }

        private void modifyServerCapital() {
            TestCapitalBuilding component = getServerComponent(TestCapitalBuilding.class).get();
            sEngineLock.lock();
            component.setBooleanSyncMe(true);
            component.setStringSyncMeAlso("Goodbye World");
            sEngineLock.unlock();
        }

        private void testSubmitRequest() {
            testCapitalSpawnDefaultServer();
            TestCapitalBuilding cap = getClientComponent(TestCapitalBuilding.class).get();
            sEngineLock.lock();
            cap.getGameObject()
                    .addComponent(
                            new LambdaOnStart(
                                    () -> {
                                        cap.mPasswordRequest.invoke(
                                                new TestAttackData(
                                                        TestCapitalBuilding.CORRECT_PASSWORD, 354));
                                    }));
            sEngineLock.unlock();
            await().atMost(TIMEOUT, SECONDS)
                    .until(
                            () -> {
                                sEngineLock.lock();
                                boolean ret = cap.getClientToggled().get() == 354;
                                sEngineLock.unlock();
                                return ret;
                            });
        }

        private void testCanDestroy() {
            testCapitalSpawnDefaultServer();
            TestCapitalBuilding cap = getServerComponent(TestCapitalBuilding.class).get();
            sEngineLock.lock();
            cap.getGameObject()
                    .addComponent(new LambdaOnStart(() -> cap.getGameObject().destroy()));
            sEngineLock.unlock();
            await().atMost(TIMEOUT, SECONDS)
                    .until(
                            () -> {
                                sEngineLock.lock();
                                boolean ret = getClientComponent(TestCapitalBuilding.class) == null;
                                sEngineLock.unlock();
                                return ret;
                            });
        }

        private void testSetOwnerId() {
            testCapitalSpawnDefaultServer();
            TestCapitalBuilding cap = getServerComponent(TestCapitalBuilding.class).get();
            TestCapitalBuilding clientCap = getClientComponent(TestCapitalBuilding.class).get();
            sEngineLock.lock();
            int ownerId = cap.getNetworkObject().getOwnerId();
            int clientOwnerId = clientCap.getNetworkObject().getOwnerId();
            cap.getNetworkObject().setOwnerId(ownerId + 1);
            assert (ownerId != cap.getNetworkObject().getOwnerId());
            sEngineLock.unlock();
            assert (ownerId == clientOwnerId);
            await().atMost(TIMEOUT, SECONDS)
                    .until(
                            () -> {
                                sEngineLock.lock();
                                boolean ret = clientCap.getNetworkObject().getOwnerId() != ownerId;
                                sEngineLock.unlock();
                                return ret;
                            });
        }
    }

    @Test
    public void testCapitalSpawnedServer() throws Throwable {
        try (TestContext ctx = new TestContext(7002)) {
            ctx.run(ctx::testCapitalSpawnDefaultServer);
        }
    }

    @Test
    public void testCapitalUpdatedServer() throws Throwable {
        try (TestContext ctx = new TestContext(7003)) {
            ctx.run(
                    () -> {
                        Reference<TestCapitalBuilding> nc = ctx.testCapitalSpawnDefaultServer();
                        ctx.modifyServerCapital();
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            sEngineLock.lock();
                                            boolean ret = nc.get().getSyncMe().get() == true;
                                            sEngineLock.unlock();
                                            return ret;
                                        });
                        sEngineLock.lock();
                        assert (nc.get().getSyncMe().get() == true);
                        sEngineLock.unlock();
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            sEngineLock.lock();
                                            boolean ret =
                                                    nc.get()
                                                            .getSyncMeAlso()
                                                            .get()
                                                            .equals("Goodbye World");
                                            sEngineLock.unlock();
                                            return ret;
                                        });
                        sEngineLock.lock();
                        assert (nc.get().getSyncMeAlso().get().equals("Goodbye World"));
                        sEngineLock.unlock();
                    });
        }
    }

    @Test
    public void testCapitalUpdatedClient() throws Throwable {
        try (TestContext ctx = new TestContext(7005)) {
            ctx.run(
                    () -> {
                        ctx.testCapitalSpawnDefaultServer();
                        ctx.modifyServerCapital();
                        Reference<TestCapitalBuilding> nc =
                                ctx.getClientComponent(TestCapitalBuilding.class);
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            sEngineLock.lock();
                                            boolean ret = nc.get().getSyncMe().get() == true;
                                            sEngineLock.unlock();
                                            return ret;
                                        });
                        sEngineLock.lock();
                        assert (nc.get().getSyncMe().get() == true);
                        sEngineLock.unlock();
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            sEngineLock.lock();
                                            boolean ret =
                                                    nc.get()
                                                            .getSyncMeAlso()
                                                            .get()
                                                            .equals("Goodbye World");
                                            sEngineLock.unlock();
                                            return ret;
                                        });
                        sEngineLock.lock();
                        assert (nc.get().getSyncMeAlso().get().equals("Goodbye World"));
                        sEngineLock.unlock();
                    });
        }
    }

    @Test
    public void testComponentCanSubmitActionRequest() throws Throwable {
        try (TestContext ctx = new TestContext(7006)) {
            ctx.run(ctx::testSubmitRequest);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testCanDestroy() throws Throwable {
        try (TestContext ctx = new TestContext(7007)) {
            ctx.run(ctx::testCanDestroy);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testSetOwnerId() throws Throwable {
        try (TestContext ctx = new TestContext(7007)) {
            ctx.run(ctx::testSetOwnerId);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}
