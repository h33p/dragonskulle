/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.util.UUID;
import org.dragonskulle.network.NetworkClient;

/** The NetworkObject deals with any networked variables. It can se */
public class NetworkObject {

    NetworkObject(NetworkClient client) {
        objectID = UUID.randomUUID().toString();
        isDormant = false;
        owner = client;
    }

    public void dispose() {
        this.owner.dispose();
    }
    /** The UUID of the object. */
    String objectID;

    /** The Client Connection to the server */
    final NetworkClient owner;

    /**
     * if True, then the server will not accept commands from the object. It can still receive
     * commands.
     */
    boolean isDormant;

    /**
     * Unsure of the usage for this method //TODO implement
     * @param newOwner The new owner to be transferred to.
     */
    private void transferOwnership(NetworkClient newOwner) {}
}
