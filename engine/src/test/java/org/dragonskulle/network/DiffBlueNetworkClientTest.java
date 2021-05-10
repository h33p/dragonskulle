/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class DiffBlueNetworkClientTest {
    @Test
    public void testConstructor() {
        NetworkClient actualNetworkClient = new NetworkClient("127.0.0.1", 8080, null);
        assertTrue(actualNetworkClient.isConnected());
    }

    @Test
    public void testDispose() {
        NetworkClient networkClient = new NetworkClient("127.0.0.1", 8080, null);
        networkClient.dispose();
        assertTrue(networkClient.getDataOut() instanceof NetworkMessageStream);
        assertFalse(networkClient.isConnected());
    }
}
