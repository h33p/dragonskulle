/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.util.*;

import org.dragonskulle.network.ClientInstance;
import org.dragonskulle.network.NetworkMessage;

/**
 * The NetworkObject deals with any networked variables.
 */
public class NetworkObject {
    public NetworkObject(
            ClientInstance client,
            ServerBroadcastCallback broadcastCallback,
            SendBytesToClientCallback clientCallback) {
        owner = client;
        serverBroadcastCallback = broadcastCallback;
        sendBytesToClientCallback = clientCallback;
    }

    /**
     * Possibly a temporary function to edit a network object without a reference.
     */

    public Networkable get(int i) {
        return this.children.get(i);
    }

    public Networkable get(Networkable n) {
        return this.children.get(this.children.indexOf(n));
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
     * A callback to broadcast a message to all clients
     */
    public interface ServerBroadcastCallback {
        void call(byte[] bytes);
    }

    /**
     * A callback to broadcast a message to a SINGLE clients,this client is the owner
     */
    public interface SendBytesToClientCallback {
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
    public void addChild(Networkable child) {
        this.children.add(child);
    }

    /**
     * Add multiple networkable children to network object
     *
     * @param children The networkable components to be added
     */
    public void addChildren(Networkable[] children) {
        Collections.addAll(this.children, children);
    }

    /**
     * Spawns a component and notifies all clients using callback.
     *
     * @param component   The component to be spawned, must extend Networkable
     * @param messageCode The message code of the spawn.
     */
    private void spawnComponent(Networkable component, byte messageCode) {
        System.out.println("spawning component on all clients");
        byte[] spawnComponentBytes;
        byte[] componentBytes = component.serializeFully();
        System.out.println("component bytes to spawn :: " + Arrays.toString(componentBytes));
        System.out.println("component bytes : " + componentBytes.length);
        spawnComponentBytes = NetworkMessage.build(messageCode, componentBytes);
        serverBroadcastCallback.call(spawnComponentBytes);
        addChild(component);
    }

    /**
     * Spawns a capital using the @link{spawnComponent} method
     */
    public void spawnCapital() {
        Capital capital = new Capital();
        capital.connectSyncVars();
        spawnComponent(capital, (byte) 21);
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
    private final ArrayList<Networkable> children = new ArrayList<>();

    /**
     * The UUID of the object.
     */
    public String networkObjectId = UUID.randomUUID().toString();

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
        for (Networkable child : this.children) {
            if (child.hasBeenModified()) {
                System.out.println("child has been modified in a networkobject");
                serverBroadcastCallback.call(NetworkMessage.build((byte) 10, child.serialize()));
            }
        }
    }
}
