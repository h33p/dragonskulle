/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.dragonskulle.network.ClientInstance;
import org.dragonskulle.network.NetworkMessage;

/**
 * @author Oscar L The NetworkObject deals with any networked variables.
 */
public class NetworkObject {
    /**
     * The UUID of the object.
     */
    public int networkObjectId;
    private final AtomicInteger mNetworkComponentCounter = new AtomicInteger(0);

    /**
     * Instantiates a new Network object.
     *
     * @param client            the client
     * @param broadcastCallback the broadcast callback
     * @param clientCallback    the client callback
     */
    public NetworkObject(
            int id,
            ClientInstance client,
            ServerBroadcastCallback broadcastCallback,
            SendBytesToClientCallback clientCallback) {
        networkObjectId = id;
        owner = client;
        serverBroadcastCallback = broadcastCallback;
        sendBytesToClientCallback = clientCallback;
    }

//    /**
//     * Possibly a temporary function to edit a network object without a reference. @param i the
//     *
//     * @param i the
//     * @return the networkable
//     */
//    public NetworkableComponent get(int i) {
//        return this.children.get(i);
//    }

    /**
     * Get networkable.
     *
     * @param n the n
     * @return the networkable
     */
    public NetworkableComponent get(NetworkableComponent n) {
        return this.children.get(this.children.indexOf(n));
    }

    /**
     * Finds a networkable component by id.
     *
     * @param componentId the id
     * @return the networkable
     */
    public NetworkableComponent findComponent(int componentId) {
        return this.children.stream()
                .filter(e -> e.getId() == componentId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkObject that = (NetworkObject) o;
        return Objects.equals(networkObjectId, that.networkObjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkObjectId);
    }

    /**
     * Get networkable.
     *
     * @param id the id
     * @return the networkable
     */
    public NetworkableComponent get(int id) {
        return this.children.stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null); // will return null if not found
    }

    public ArrayList<NetworkableComponent> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return "NetworkObject{"
                + "children="
                + children
                + ", networkObjectId='"
                + networkObjectId
                + '\''
                + ", owner="
                + owner
                + ", isDormant="
                + isDormant
                + '}';
    }

    /**
     * A callback to broadcast a message to all clients
     */
    public interface ServerBroadcastCallback {
        /**
         * Call.
         *
         * @param bytes the bytes
         */
        void call(byte[] bytes);
    }

    /**
     * A callback to broadcast a message to a SINGLE clients,this client is the owner
     */
    public interface SendBytesToClientCallback {
        /**
         * Call.
         *
         * @param client the client
         * @param bytes  the bytes
         */
        void call(ClientInstance client, byte[] bytes);
    }

    /**
     * Stores the broadcast callback
     */
    private final ServerBroadcastCallback serverBroadcastCallback;

    /**
     * Stores the single sender callback
     */
    private final SendBytesToClientCallback sendBytesToClientCallback;

    /**
     * Add a networkable child to network object
     *
     * @param child The networkable component to be added
     */
    public void addChild(NetworkableComponent child) {
        this.children.add(child);
    }

    /**
     * Add multiple networkable children to network object
     *
     * @param children The networkable components to be added
     */
    public void addChildren(NetworkableComponent[] children) {
        Collections.addAll(this.children, children);
    }

    /**
     * Spawns a component and notifies all clients using callback.
     *
     * @param component   The component to be spawned, must extend NetworkableComponent
     * @param messageCode The message code of the spawn.
     * @return The ID of the spawned component
     */
    private int spawnComponent(NetworkableComponent component, byte messageCode) {
        System.out.println("spawning component on all clients");
        byte[] spawnComponentBytes;
        byte[] componentBytes = component.serializeFully();
        //        System.out.println("component bytes to spawn :: " +
        // Arrays.toString(componentBytes));
        //        System.out.println("component bytes : " + componentBytes.length);
        spawnComponentBytes = NetworkMessage.build(messageCode, componentBytes);
        serverBroadcastCallback.call(spawnComponentBytes);
        addChild(component);
        return component.getId();
    }

    /**
     * Spawns a capital using the @link{spawnComponent} method
     *
     * @return The id of the spawned component
     */
    public int spawnCapital() {
        Capital capital = new Capital(this.allocateId());
        capital.connectSyncVars();
        return spawnComponent(capital, (byte) 21);
    }

    /**
     * Sends the map to the client, this is called on connect
     *
     * @param mapBytes The bytes of the serialized map.
     */
    public void spawnMap(byte[] mapBytes) {
        System.out.println("spawning map on client");
        System.out.println("Map bytes :: " + mapBytes.length + "bytes");
        byte[] spawnMapMessage = NetworkMessage.build((byte) 20, mapBytes);
        sendBytesToClientCallback.call(owner, spawnMapMessage);
    }

    /**
     * Children of the object will be networkable and updated on clients
     */
    private final ArrayList<NetworkableComponent> children = new ArrayList<>();

    /**
     * Gets network object id.
     *
     * @return the network object id
     */
    public int getNetworkObjectId() {
        return networkObjectId;
    }

    /**
     * The Client Connection to the server
     */
    final ClientInstance owner;

    /**
     * if True, then the server will not accept commands from the object. It can still receive
     * commands.
     */
    boolean isDormant = false;

    /**
     * Broadcasts updates all of the modified children
     */
    public void broadcastUpdate() {
        for (NetworkableComponent child : this.children) {
            if (child.hasBeenModified()) {
                System.out.println("child has been modified in a networkobject");
                serverBroadcastCallback.call(NetworkMessage.build((byte) 10, child.serialize()));
            }
        }
    }

    private int allocateId() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.networkObjectId);
        sb.append(this.mNetworkComponentCounter.getAndIncrement());
        return Integer.parseInt(sb.toString());
    }
}
