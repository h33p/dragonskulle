/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkableComponent;
import org.junit.*;
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
                            handle.addComponent(new Capital());
                        }));

        CLIENT_NETMAN_SCENE.addRootObject(
                new GameObject("netman", handle -> handle.addComponent(CLIENT_NETWORK_MANAGER)));
        SERVER_NETMAN_SCENE.addRootObject(
                new GameObject("netman", handle -> handle.addComponent(SERVER_NETWORK_MANAGER)));
    }

    private static ReentrantLock mEngineLock = new ReentrantLock();

    private static void cleanupNetmans() {
        log.info("Cleanup netmans");
        if (CLIENT_NETWORK_MANAGER.getClientManager() != null)
            CLIENT_NETWORK_MANAGER.getClientManager().disconnect();
        if (SERVER_NETWORK_MANAGER.getServerManager() != null)
            SERVER_NETWORK_MANAGER.getServerManager().destroy();
        log.info("CLEANED UP");
    }

    private static class LambdaOnStart extends Component implements IOnStart {
        IOnStart mHandler;

        public LambdaOnStart(IOnStart handler) {
            mHandler = handler;
        }

        @Override
        public void onStart() {
            mHandler.onStart();
        }

        @Override
        protected void onDestroy() {}
    }

    private static class TestContext implements NativeResource {

        private Thread mTestThread;
        private boolean mShouldExit;
        private int mPort;

        private Throwable toThrow = null;

        public TestContext(int port) {
            mPort = port;
        }

        public synchronized void run(Runnable runnable) throws Throwable {
            mTestThread =
                    new Thread(
                            () -> {
                                try {
                                    runnable.run();
                                } finally {
                                    mShouldExit = true;
                                }
                            });
            mEngineLock.lock();

            cleanupNetmans();

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

            mTestThread.setUncaughtExceptionHandler(
                    (t, e) -> {
                        toThrow = e;
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

            if (toThrow != null) throw toThrow;
        }

        private boolean shouldExit() {

            mEngineLock.unlock();

            if (mShouldExit) return false;

            mEngineLock.lock();

            return true;
        }

        @Override
        public void free() {
            mEngineLock.lock();
            cleanupNetmans();
            mShouldExit = true;
            mEngineLock.unlock();
        }

        public <T extends NetworkableComponent> Reference<T> getServerComponent(Class<T> type) {
            mEngineLock.lock();
            Reference<T> ret =
                    SERVER_NETWORK_MANAGER.getServerManager().getNetworkObjects().values().stream()
                            .map(c -> c.getNetworkObject().get().getGameObject().getComponent(type))
                            .filter(c -> c != null)
                            .findFirst()
                            .orElse(null);
            mEngineLock.unlock();
            return ret;
        }

        public <T extends NetworkableComponent> Reference<T> getClientComponent(Class<T> type) {
            mEngineLock.lock();
            Reference<T> ret =
                    CLIENT_NETWORK_MANAGER
                            .getClientManager()
                            .getNetworkObjects()
                            .map(c -> c.get().getGameObject().getComponent(type))
                            .filter(c -> c != null)
                            .findFirst()
                            .orElse(null);
            mEngineLock.unlock();
            return ret;
        }

        private Reference<Capital> testCapitalSpawnDefaultServer() {
            await().atMost(6, SECONDS).until(() -> getServerComponent(Capital.class) != null);
            await().atMost(TIMEOUT * 2, SECONDS)
                    .until(() -> getClientComponent(Capital.class) != null);

            Reference<Capital> clientCapital = getClientComponent(Capital.class);
            assertNotNull(clientCapital);
            Reference<Capital> serverCapital = getServerComponent(Capital.class);
            assertNotNull(serverCapital);

            mEngineLock.lock();
            int capitalId = clientCapital.get().getNetworkObject().getId();
            log.info(
                    "\t-----> " + capitalId + " " + serverCapital.get().getNetworkObject().getId());
            assertEquals(capitalId, serverCapital.get().getNetworkObject().getId());
            log.info("\t-----> " + capitalId);
            assert (serverCapital.get().getSyncMe().get() == false);
            assert (serverCapital.get().getSyncMeAlso().get().equals("Hello World"));
            mEngineLock.unlock();
            return serverCapital;
        }

        private void modifyServerCapital() {
            Capital component = getServerComponent(Capital.class).get();
            mEngineLock.lock();
            component.setBooleanSyncMe(true);
            component.setStringSyncMeAlso("Goodbye World");
            mEngineLock.unlock();
        }

        private void testSubmitRequest() {
            testCapitalSpawnDefaultServer();
            Capital cap = getClientComponent(Capital.class).get();
            mEngineLock.lock();
            cap.getGameObject()
                    .addComponent(
                            new LambdaOnStart(
                                    () -> {
                                        cap.mPasswordRequest.invoke(
                                                new TestAttackData(Capital.CORRECT_PASSWORD, 354));
                                    }));
            mEngineLock.unlock();
            await().atMost(TIMEOUT, SECONDS)
                    .until(
                            () -> {
                                mEngineLock.lock();
                                boolean ret = cap.getClientToggled().get() == 354;
                                mEngineLock.unlock();
                                return ret;
                            });
        }

        private void testCanDestroy() {
            testCapitalSpawnDefaultServer();
            Capital cap = getServerComponent(Capital.class).get();
            mEngineLock.lock();
            cap.getGameObject()
                    .addComponent(new LambdaOnStart(() -> cap.getGameObject().destroy()));
            mEngineLock.unlock();
            await().atMost(TIMEOUT, SECONDS)
                    .until(
                            () -> {
                                mEngineLock.lock();
                                boolean ret = getClientComponent(Capital.class) == null;
                                mEngineLock.unlock();
                                return ret;
                            });
        }

        private void testSetOwnerId() {
            testCapitalSpawnDefaultServer();
            Capital cap = getServerComponent(Capital.class).get();
            Capital clientCap = getClientComponent(Capital.class).get();
            mEngineLock.lock();
            int ownerId = cap.getNetworkObject().getOwnerId();
            int clientOwnerId = clientCap.getNetworkObject().getOwnerId();
            cap.getNetworkObject().setOwnerId(ownerId + 1);
            assert (ownerId != cap.getNetworkObject().getOwnerId());
            mEngineLock.unlock();
            assert (ownerId == clientOwnerId);
            await().atMost(TIMEOUT, SECONDS)
                    .until(
                            () -> {
                                mEngineLock.lock();
                                boolean ret = clientCap.getNetworkObject().getOwnerId() != ownerId;
                                mEngineLock.unlock();
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
                        Reference<Capital> nc = ctx.testCapitalSpawnDefaultServer();
                        ctx.modifyServerCapital();
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            mEngineLock.lock();
                                            boolean ret = nc.get().getSyncMe().get() == true;
                                            mEngineLock.unlock();
                                            return ret;
                                        });
                        mEngineLock.lock();
                        assert (nc.get().getSyncMe().get() == true);
                        mEngineLock.unlock();
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            mEngineLock.lock();
                                            boolean ret =
                                                    nc.get()
                                                            .getSyncMeAlso()
                                                            .get()
                                                            .equals("Goodbye World");
                                            mEngineLock.unlock();
                                            return ret;
                                        });
                        mEngineLock.lock();
                        assert (nc.get().getSyncMeAlso().get().equals("Goodbye World"));
                        mEngineLock.unlock();
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
                        Reference<Capital> nc = ctx.getClientComponent(Capital.class);
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            mEngineLock.lock();
                                            boolean ret = nc.get().getSyncMe().get() == true;
                                            mEngineLock.unlock();
                                            return ret;
                                        });
                        mEngineLock.lock();
                        assert (nc.get().getSyncMe().get() == true);
                        mEngineLock.unlock();
                        await().atMost(TIMEOUT, SECONDS)
                                .until(
                                        () -> {
                                            mEngineLock.lock();
                                            boolean ret =
                                                    nc.get()
                                                            .getSyncMeAlso()
                                                            .get()
                                                            .equals("Goodbye World");
                                            mEngineLock.unlock();
                                            return ret;
                                        });
                        mEngineLock.lock();
                        assert (nc.get().getSyncMeAlso().get().equals("Goodbye World"));
                        mEngineLock.unlock();
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
