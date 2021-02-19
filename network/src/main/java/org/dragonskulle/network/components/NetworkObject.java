package org.dragonskulle.network.components;

import org.dragonskulle.network.NetworkClient;

/**
 * The NetworkObject deals with any networked variables. It can se
 */
public class NetworkObject {
    /**
     * The UUID of the object.
     */
    int netID;

    /**
     * The Client Connection to the server
     */
    NetworkClient owner;

    /**
     * if True, then the server will not accept commands from the object. It can still receive commands.
     */
    boolean isDormant;

    /**
     * Unsure of the usage for this method //TODO implement
     * @param newOwner The new owner to be transferred to.
     */
    private void transferOwnership(NetworkClient newOwner) {

    }

//TODO unsure of usage
//    private void serialize(targetClient, writer){
//
//    }
//
//    private serializeObject(NetworkMessage obj){
//    }
//
//    private void deserialize(reader)
//
    void registerSyncVar(ISyncVar syncVar){};

}
