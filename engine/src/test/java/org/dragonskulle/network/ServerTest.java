/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

import java.util.logging.Logger;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
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
        mServerInstance = new StartServer();
        mClientListener = new ClientEars();
    }

    @AfterClass
    public static void tearDown() {
        mServerInstance.dispose();
    }

    @Test
    public void testSpawnMap() {
        mNetworkClient = new NetworkClient("127.0.0.1", 7000, mClientListener);
        await().atMost(8, SECONDS).until(() -> mNetworkClient.hasMap());
        mNetworkClient.dispose();
    }

    @Test
    public void testCapitalSpawned() {
        mNetworkClient = new NetworkClient("127.0.0.1", 7000, mClientListener);
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasMap());
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasCapital());
        assertFalse(mServerInstance.server.networkObjects.isEmpty());
        String capitalId = mNetworkClient.getCapitalId();
        final Capital nc = (Capital) mServerInstance.server.findComponent(capitalId);
        assertNotNull(nc);
        mLogger.info("\t-----> " + capitalId);
        assert (nc.getSyncMe().get() == false);
        assert (nc.getSyncMeAlso().get().equals("Hello World"));
        mNetworkClient.dispose();
    }

    @Test
    public void testCapitalUpdated() {
        mNetworkClient = new NetworkClient("127.0.0.1", 7000, mClientListener);
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasMap());
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> mNetworkClient.hasCapital());
        assertFalse(mServerInstance.server.networkObjects.isEmpty());
        NetworkObject object = mServerInstance.server.networkObjects.get(0);
        assertNotNull(object);
        String capitalId = mNetworkClient.getCapitalId();
        assertNotNull(capitalId);
        mLogger.info("\t-----> " + capitalId);
        final Capital nc = (Capital) mServerInstance.server.findComponent(capitalId);
        assert (nc.getSyncMe().get() == false);
        assert (nc.getSyncMeAlso().get().equals("Hello World"));
        await().atMost(TIMEOUT, SECONDS).until(() -> nc.getSyncMe().get() == true);
        assert (nc.getSyncMe().get().equals(true));
        await().atMost(TIMEOUT, SECONDS)
                .until(() -> nc.getSyncMeAlso().get().equals("Goodbye World"));
        assert (nc.getSyncMeAlso().get().equals("Goodbye World"));
        mNetworkClient.dispose();
    }
}
