package org.dragonskulle.network.components;

import org.dragonskulle.network.NetworkClient;

import java.util.UUID;

/**
 * The NetworkObject deals with any networked variables. It can se
 */
public class NetworkObject {

    NetworkObject(NetworkClient client){
        netID = UUID.randomUUID().toString();
        isDormant = false;
        owner = client;
    }

    public void dispose(){
        this.owner.dispose();
    }
    /**
     * The UUID of the object.
     */
    String netID;

    /**
     * The Client Connection to the server
     */
    final NetworkClient owner;

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

//    void registerSyncVar(ISyncVar syncVar) {
//        this.owner.registerSyncVarWithServer(syncVar, isDormant); //send message to server
//        this.synced.add(syncVar); //should add if server accepts sync
//    }

    void registerSyncVars(byte[] payload) {
        this.owner.registerSyncVarsWithServer(payload); //send message to server
    }


    public <T> void notifySyncedOfUpdate(ISyncVar<T> newValue) {
        this.owner.notifySyncedOfUpdate(netID, isDormant, newValue);
    }

    public Object getSynced(String id) {
        return this.owner.getSynced(id);
    }
}
