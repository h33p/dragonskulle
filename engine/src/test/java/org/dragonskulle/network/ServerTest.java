/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.dragonskulle.core.Reference;
import org.dragonskulle.network.components.Capital.Capital;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.TestAttackData;
import org.junit.*;
import org.lwjgl.system.NativeResource;

/** @author Oscar L */
public class ServerTest {
    private static final Logger mLogger = Logger.getLogger(ServerTest.class.getName());
    private static final long TIMEOUT = 8;

    private static class TestContext implements NativeResource {
        private IServerListener mServerListener;
        private Server mServerInstance;
        private ClientEars mClientListener;
        private NetworkClient mNetworkClient;
        private ClientNetworkManager clientNetworkManager;

        public TestContext(int port) {
            mServerListener = new ServerEars();
            mServerInstance = new Server(port, mServerListener, true, new AtomicInteger(0));
            mClientListener = new ClientEars();
            mNetworkClient = new NetworkClient("127.0.0.1", port, mClientListener, false);
            clientNetworkManager = mNetworkClient.createNetworkManager();
            mServerInstance.startFixedUpdateDetachedFromGame();
        }

        public TestContext(int port, boolean serverAutoProcess) {
            mServerListener = new ServerEars();
            mServerInstance =
                    new Server(port, mServerListener, serverAutoProcess, new AtomicInteger(0));
            mClientListener = new ClientEars();
            mNetworkClient = new NetworkClient("127.0.0.1", port, mClientListener, false);
            clientNetworkManager =
                    new ClientNetworkManager(
                            mNetworkClient::processRequests,
                            mNetworkClient::sendBytes,
                            mNetworkClient::dispose);
            //            mServerInstance.startFixedUpdateDetachedFromGame();
        }

        @Override
        public void free() {
            mNetworkClient.dispose();
            mServerInstance.dispose();
        }

        public <T extends NetworkableComponent> Reference<T> getServerComponent(Class<T> type) {
            return mServerInstance.getNetworkObjects().values().stream()
                    .map(c -> c.get().getGameObject().getComponent(type))
                    .filter(c -> c != null)
                    .findFirst()
                    .orElse(null);
        }

        public <T extends NetworkableComponent> Reference<T> getClientComponent(Class<T> type) {
            return mNetworkClient.getNetworkableObjects().values().stream()
                    .map(c -> c.get().getGameObject().getComponent(type))
                    .filter(c -> c != null)
                    .findFirst()
                    .orElse(null);
        }

        private void testMapClient() {
            await().atMost(6, SECONDS).until(() -> mNetworkClient.hasRequests());
            //        mLogger.info("Requests: " + )
            mNetworkClient.processSingleRequest();
            // await().atMost(3, SECONDS).until(() -> mNetworkClient.hasMap());
        }

        private Reference<Capital> testCapitalSpawnDefaultServer() {
            await().atMost(6, SECONDS).until(() -> mNetworkClient.hasRequests());
            mNetworkClient.processSingleRequest();
            // await().atMost(1800, MILLISECONDS).until(() -> mNetworkClient.hasMap());
            await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasCapital());
            assertFalse(mServerInstance.mNetworkObjects.isEmpty());
            Reference<Capital> clientCapital = getClientComponent(Capital.class);
            assertNotNull(clientCapital);
            Reference<Capital> serverCapital = getServerComponent(Capital.class);
            assertNotNull(serverCapital);
            int capitalId = clientCapital.get().getNetworkObject().getId();
            mLogger.info(
                    "\t-----> " + capitalId + " " + serverCapital.get().getNetworkObject().getId());
            assertEquals(capitalId, serverCapital.get().getNetworkObject().getId());
            mLogger.info("\t-----> " + capitalId);
            assert (serverCapital.get().getSyncMe().get() == false);
            assert (serverCapital.get().getSyncMeAlso().get().equals("Hello World"));
            return serverCapital;
        }

        private void modifyServerCapital() {
            Capital component = getServerComponent(Capital.class).get();
            component.setBooleanSyncMe(true);
            component.setStringSyncMeAlso("Goodbye World");
        }

        private Reference<Capital> testCapitalSpawnDefaultClient() {
            await().atMost(6, SECONDS).until(() -> mNetworkClient.hasRequests());
            mNetworkClient.processSingleRequest();
            // await().atMost(1, SECONDS).until(() -> mNetworkClient.hasMap());
            await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasCapital());
            assertFalse(mServerInstance.mNetworkObjects.isEmpty());
            Reference<Capital> clientCapital = getClientComponent(Capital.class);
            assertNotNull(clientCapital);
            int capitalId = clientCapital.get().getNetworkObject().getId();
            mLogger.info("Capital ID : " + capitalId);
            mLogger.info("mClient has these objects : " + mNetworkClient.getNetworkableObjects());
            mLogger.info("\t-----> " + capitalId);
            assert (clientCapital.get().getSyncMe().get() == false);
            assert (clientCapital.get().getSyncMeAlso().get().equals("Hello World"));
            return clientCapital;
        }
    }

    @Test
    public void testSpawnMap() {
        try (TestContext ctx = new TestContext(7001)) {
            ctx.testMapClient();
        }
    }

    @Test
    public void testCapitalSpawnedServer() {
        try (TestContext ctx = new TestContext(7002)) {
            ctx.testMapClient();
            ctx.testCapitalSpawnDefaultServer();
        }
    }

    @Test
    public void testCapitalUpdatedServer() {
        try (TestContext ctx = new TestContext(7003)) {
            ctx.testMapClient();

            Reference<Capital> nc = ctx.testCapitalSpawnDefaultServer();
            ctx.modifyServerCapital();
            await().atMost(6, SECONDS).until(() -> ctx.mNetworkClient.hasRequests());
            ctx.mNetworkClient.processSingleRequest();
            await().atMost(TIMEOUT, SECONDS).until(() -> nc.get().getSyncMe().get() == true);
            assert (nc.get().getSyncMe().get() == true);
            await().atMost(TIMEOUT, SECONDS)
                    .until(() -> nc.get().getSyncMeAlso().get().equals("Goodbye World"));
            assert (nc.get().getSyncMeAlso().get().equals("Goodbye World"));
        }
    }

    @Test
    public void testCapitalSpawnedClient() {
        try (TestContext ctx = new TestContext(7004)) {
            ctx.testMapClient();
            ctx.testCapitalSpawnDefaultClient();
        }
    }

    @Test
    public void testCapitalUpdatedClient() {
        try (TestContext ctx = new TestContext(7005)) {
            ctx.testMapClient();
            Reference<Capital> nc = ctx.testCapitalSpawnDefaultClient();
            ctx.modifyServerCapital();
            await().atMost(1800, MILLISECONDS)
                    .until(() -> ctx.mNetworkClient.setProcessMessagesAutomatically(true));
            await().atMost(TIMEOUT, SECONDS).until(() -> nc.get().getSyncMe().get() == true);
            assert (nc.get().getSyncMe().get() == true);
            await().atMost(TIMEOUT, SECONDS)
                    .until(() -> nc.get().getSyncMeAlso().get().equals("Goodbye World"));
            assert (nc.get().getSyncMeAlso().get().equals("Goodbye World"));
        }
    }

    @Test
    public void testComponentCanSubmitActionRequest() {
        try (TestContext ctx = new TestContext(7006, true)) {
            ctx.mServerInstance.startFixedUpdateDetachedFromGame();
            ctx.testMapClient();
            Capital cap = ctx.testCapitalSpawnDefaultClient().get();
            cap.mPasswordRequest.invoke(new TestAttackData(Capital.CORRECT_PASSWORD, 354));
            ctx.mServerInstance.processRequests();
            ctx.mNetworkClient.setProcessMessagesAutomatically(true);
            await().atMost(TIMEOUT, SECONDS).until(() -> cap.getClientToggled().get() == 354);
        }
    }
}
