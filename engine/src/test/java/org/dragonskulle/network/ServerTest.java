/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

import java.util.logging.Logger;
import org.dragonskulle.network.components.Capital;
import org.junit.*;

/** @author Oscar L */
public class ServerTest {
    private static final Logger mLogger = Logger.getLogger(ServerTest.class.getName());
    private static final long TIMEOUT = 8;
    private static StartServer mServerInstance;
    private static ServerEars mServerListener;
    private static NetworkClient mNetworkClient;
    private static ClientEars mClientListener;

    @BeforeClass
    public static void setUp() {
        mServerInstance = new StartServer(true);
        mClientListener = new ClientEars();
    }

    @AfterClass
    public static void tearDown() {
        mServerInstance.dispose();
    }

    @Test
    public void testSpawnMap() {
        mNetworkClient = new NetworkClient("127.0.0.1", 7000, mClientListener, false);
        testMap();
        mNetworkClient.dispose();
    }

    private void testMap() {
        await().atMost(3, SECONDS).until(() -> mNetworkClient.hasRequests());
        mNetworkClient.processSingleRequest();
        await().atMost(1, SECONDS).until(() -> mNetworkClient.hasMap());
    }

    @Test
    public void testCapitalSpawned() {
        mNetworkClient = new NetworkClient("127.0.0.1", 7000, mClientListener, false);
        testMap();
        testCapitalSpawnDefault();
        mNetworkClient.dispose();
    }

    private Capital testCapitalSpawnDefault() {
        await().atMost(3, SECONDS).until(() -> mNetworkClient.hasRequests());
        mNetworkClient.processSingleRequest();
        await().atMost(1, SECONDS).until(() -> mNetworkClient.hasMap());
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasCapital());
        assertFalse(mServerInstance.server.networkObjects.isEmpty());
        int capitalId = mNetworkClient.getCapitalId();
        final Capital nc = (Capital) mServerInstance.server.findComponent(capitalId);
        assertNotNull(nc);
        mLogger.info("\t-----> " + capitalId);
        assert (nc.getSyncMe().get() == false);
        assert (nc.getSyncMeAlso().get().equals("Hello World"));
        return nc;
    }

    @Test
    public void testCapitalUpdated() {
        mNetworkClient = new NetworkClient("127.0.0.1", 7000, mClientListener, false);
        testMap();
        Capital nc = testCapitalSpawnDefault();

        await().atMost(3, SECONDS).until(() -> mNetworkClient.hasRequests());
        mNetworkClient.processSingleRequest();
        await().atMost(TIMEOUT, SECONDS).until(() -> nc.getSyncMe().get() == true);
        assert (nc.getSyncMe().get() == true);
        await().atMost(TIMEOUT, SECONDS)
                .until(() -> nc.getSyncMeAlso().get().equals("Goodbye World"));
        assert (nc.getSyncMeAlso().get().equals("Goodbye World"));
        mNetworkClient.dispose();
    }
}
