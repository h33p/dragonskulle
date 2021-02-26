/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.net.InetAddress;

public class ClientInstance {
    public final InetAddress IP;
    public final int PORT;

    public ClientInstance(InetAddress ip, int port) {
        this.IP = ip;
        this.PORT = port;
    }

    @Override
    public String toString() {
        return IP.toString() + ":" + PORT;
    }
}
