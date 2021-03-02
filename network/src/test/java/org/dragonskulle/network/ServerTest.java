package org.dragonskulle.network;

import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.junit.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Oscar L
 */
public class ServerTest {
    private static final long TIMEOUT = 8;
    private static StartServer serverInstance;
    private static ServerEars serverListener;
    private static NetworkClient networkClient;
    private static ClientEars clientListener;

    @BeforeClass
    public static void setUp() {
        serverInstance = new StartServer();
        clientListener = new ClientEars();
    }

    @AfterClass
    public static void tearDown() {
        serverInstance.dispose();
    }

    @Test
    public void testSpawnMap() {
        networkClient = new NetworkClient("127.0.0.1", 7000, clientListener);
        await().atMost(8, SECONDS).until(() -> networkClient.hasMap());
        networkClient.dispose();
    }

    @Test
    public void testCapitalSpawned() {
        networkClient = new NetworkClient("127.0.0.1", 7000, clientListener);
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> networkClient.hasMap());
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> networkClient.hasCapital());
        assertFalse(serverInstance.server.networkObjects.isEmpty());
        NetworkObject object = serverInstance.server.networkObjects.get(0);
        assertNotNull(object);
        String capitalId = networkClient.getCapitalId();
        assertNotNull(capitalId);
        Capital tmp = (Capital) object.get(capitalId);
        if (tmp == null) {
            for (int i = 0; i < 10; i++) {
                tmp = (Capital) object.get(capitalId);
                if (tmp != null) {
                    break;
                }
            }
        }
        final Capital nc = tmp;
        await().atMost(TIMEOUT, SECONDS).until(() -> nc != null);
        assert (nc.getSyncMe().get() == false);
        assert (nc.getSyncMeAlso().get().equals("Hello World"));
        networkClient.dispose();

    }

    @Test
    public void testCapitalUpdated() {
        networkClient = new NetworkClient("127.0.0.1", 7000, clientListener);
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> networkClient.hasMap());
        await().atMost(TIMEOUT * 2, SECONDS).until(() -> networkClient.hasCapital());
        assertFalse(serverInstance.server.networkObjects.isEmpty());
        NetworkObject object = serverInstance.server.networkObjects.get(0);
        assertNotNull(object);
        String capitalId = networkClient.getCapitalId();
        assertNotNull(capitalId);
        Capital tmp = (Capital) object.get(capitalId);
        if (tmp == null) {
            for (int i = 0; i < 10; i++) {
                tmp = (Capital) object.get(capitalId);
                if (tmp != null) {
                    break;
                }
            }
        }
        final Capital nc = tmp;
        await().atMost(TIMEOUT, SECONDS).until(() -> nc != null);
        assert (nc.getSyncMe().get() == false);
        assert (nc.getSyncMeAlso().get().equals("Hello World"));
        await().atMost(TIMEOUT, SECONDS).until(() -> nc.getSyncMe().get() == true);
        await().atMost(TIMEOUT, SECONDS).until(() -> nc.getSyncMeAlso().get() == "Goodbye World");
        networkClient.dispose();

    }


}