/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.java.Log;

@Log
/**
 * Utility class that can be used for host discovery on local networks
 *
 * @author Harry Stoltz
 */
public class LobbyDiscovery {

    private static final int DISCOVER_PORT = 17571;
    private static final byte[] UDP_DISCOVER_MAGIC = "HWDiscover".getBytes();
    private static final byte[] UDP_CONNECT_RESPONSE = "HWFound".getBytes();

    private static Listener mListener = null;

    /**
     * Thread that will receive UDP packets on UDP_PORT and if the data in the packet is equal to
     * the magic bytes for discovering a server, a response indicating that a server is hosted on
     * this address is sent.
     *
     * <p>This is used for hosting a lobby on local networks without needing to get and send IPs to
     * others on the network.
     */
    private static class Listener extends Thread {

        private final AtomicBoolean mRunning = new AtomicBoolean(true);

        /** Sets mRunning to false which results in the thread stopping */
        public void close() {
            mRunning.set(false);
        }

        @Override
        public void run() {
            try {
                final int discoverLength = UDP_DISCOVER_MAGIC.length;
                MulticastSocket socket = new MulticastSocket(DISCOVER_PORT);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                socket.joinGroup(group);
                log.info("Local lobby starting, waiting for UDP packets");
                while (mRunning.get()) {
                    DatagramPacket packet =
                            new DatagramPacket(new byte[discoverLength], discoverLength);
                    socket.receive(packet);
                    log.info("Received packet from " + packet.getAddress().getHostAddress());
                    if (Arrays.equals(packet.getData(), UDP_DISCOVER_MAGIC)) {
                        DatagramPacket response =
                                new DatagramPacket(
                                        UDP_CONNECT_RESPONSE,
                                        UDP_CONNECT_RESPONSE.length,
                                        packet.getAddress(),
                                        packet.getPort());
                        socket.send(response);
                        log.info("Packet was a discover packet for us, response sent");
                    } else {
                        log.info("Packet was not meant for us, or was corrupt");
                    }
                }
            } catch (SocketException e) {
                log.warning("Failed to create local lobby - Couldn't create UDP Socket");
            } catch (IOException e) {
                log.warning("Error receiving UDP packet");
            }
        }
    }

    /** Creates a new Listener thread if there isn't one already running */
    public static void openLocalLobby() {
        if (mListener == null) {
            mListener = new Listener();
            mListener.start();
        }
    }

    /** Stops the Listener thread if there is one running */
    public static void closeLocalLobby() {
        if (mListener != null) {
            mListener.close();
            mListener = null;
        }
    }

    /**
     * Attempt to discover a lobby on the local network.
     *
     * @return The INetAddress that responded to the discover packet, or null if nothing responded
     */
    public static InetAddress discoverLobby() {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName("230.0.0.0");
            DatagramPacket discoverPacket = new DatagramPacket(UDP_DISCOVER_MAGIC,
                    UDP_DISCOVER_MAGIC.length, group, DISCOVER_PORT);
            socket.send(discoverPacket);

            try {
                DatagramPacket response =
                        new DatagramPacket(UDP_CONNECT_RESPONSE, UDP_CONNECT_RESPONSE.length);
                socket.setSoTimeout(1000);
                socket.receive(response);
                if (Arrays.equals(response.getData(), UDP_CONNECT_RESPONSE)) {
                    log.info("Discovered a server!");
                    return response.getAddress();
                }
            } catch (IOException e) {
                log.warning("Error when receiving UDP response");
            }
        } catch (SocketException e) {
            log.warning("Failed to create UDP Socket");
        } catch (IOException e) {
            log.warning("Failed to broadcast discover packet");
        }
        return null;
    }
}
