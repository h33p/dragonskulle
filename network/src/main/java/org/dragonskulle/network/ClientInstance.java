/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.net.InetAddress;

/** The type Client instance. */
public class ClientInstance {
    /** The Ip. */
    public final InetAddress IP;
    /** The Port. */
    public final int PORT;

    /**
     * Instantiates a new Client instance.
     *
     * @param ip the ip
     * @param port the port
     */
    public ClientInstance(InetAddress ip, int port) {
        this.IP = ip;
        this.PORT = port;
    }

    @Override
    public String toString() {
        return IP.toString() + ":" + PORT;
    }
}
