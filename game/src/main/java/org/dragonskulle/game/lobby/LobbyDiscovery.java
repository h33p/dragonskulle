/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.java.Log;

/**
 * Utility class that can be used for host discovery on local networks.
 *
 * @author Harry Stoltz
 */
@Log
public class LobbyDiscovery {

    private static final int DISCOVER_PORT = 17571;
    private static final byte[] UDP_DISCOVER_MAGIC = "HWDiscover".getBytes();
    private static final byte[] UDP_CONNECT_RESPONSE = "HWFound".getBytes();

    private static Listener sListener = null;

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

        /** Sets mRunning to false which results in the thread stopping. */
        public void close() {
            mRunning.set(false);
        }

        @Override
        public void run() {
            try {
                final int discoverLength = UDP_DISCOVER_MAGIC.length;
                DatagramSocket socket = new DatagramSocket(DISCOVER_PORT);
                socket.setBroadcast(true);
                socket.setSoTimeout(100);
                log.fine("Local lobby starting, waiting for UDP packets");
                while (mRunning.get()) {
                    try {
                        DatagramPacket packet =
                                new DatagramPacket(new byte[discoverLength], discoverLength);
                        socket.receive(packet);
                        log.fine("Received packet from " + packet.getAddress().getHostAddress());
                        if (Arrays.equals(packet.getData(), UDP_DISCOVER_MAGIC)) {
                            DatagramPacket response =
                                    new DatagramPacket(
                                            UDP_CONNECT_RESPONSE,
                                            UDP_CONNECT_RESPONSE.length,
                                            packet.getAddress(),
                                            packet.getPort());
                            socket.send(response);
                            log.fine("Packet was a discover packet for us, response sent");
                        } else {
                            log.fine("Packet was not meant for us, or was corrupt");
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }
                socket.close();
            } catch (SocketException e) {
                log.warning("Failed to create local lobby - Couldn't create UDP Socket");
            } catch (IOException e) {
                log.warning("Error receiving UDP packet");
            }
        }
    }

    /** Creates a new Listener thread if there isn't one already running. */
    public static void openLocalLobby() {
        if (sListener != null) {
            sListener.close();
        }
        sListener = new Listener();
        sListener.start();
    }

    /** Stops the Listener thread if there is one running. */
    public static void closeLocalLobby() {
        if (sListener != null) {
            sListener.close();
            sListener = null;
        }
    }

    /**
     * Get a list of all udp broadcast addresses by iterating through all network interfaces.
     *
     * @return A list containing all broadcast addresses, or a list containing just 255.255.255.255
     *     if no specific broadcast addresses could be found.
     */
    private static ArrayList<InetSocketAddress> getBroadcastAddresses() {
        ArrayList<InetSocketAddress> addresses = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (iface.isLoopback()) {
                    continue;
                }

                for (InterfaceAddress address : iface.getInterfaceAddresses()) {
                    InetAddress broadcast = address.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    addresses.add(new InetSocketAddress(broadcast, DISCOVER_PORT));
                }
            }
        } catch (SocketException e) {
            log.warning("Failed to enumerate network interfaces");
        }

        if (addresses.size() == 0) {
            addresses.add(new InetSocketAddress("255.255.255.255", DISCOVER_PORT));
        }

        return addresses;
    }

    /**
     * Attempt to discover a lobby on the local network. Currently only broadcasts to the default
     * broadcast address.
     *
     * @return The INetAddress that responded to the broadcast, or null if nothing responded
     */
    public static InetAddress discoverLobby() {
        // TODO: Iterate through network interfaces and broadcast to all broadcast addresses

        ArrayList<InetSocketAddress> broadcastAddresses = getBroadcastAddresses();

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            for (InetSocketAddress address : broadcastAddresses) {
                DatagramPacket packet =
                        new DatagramPacket(UDP_DISCOVER_MAGIC, UDP_DISCOVER_MAGIC.length, address);
                socket.send(packet);
                log.fine("Sent discovery packet to " + address.getHostString());
            }

            try {
                DatagramPacket response =
                        new DatagramPacket(UDP_CONNECT_RESPONSE, UDP_CONNECT_RESPONSE.length);
                socket.setSoTimeout(1000);
                socket.receive(response);
                if (Arrays.equals(response.getData(), UDP_CONNECT_RESPONSE)) {
                    log.fine("Discovered a server!");
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
