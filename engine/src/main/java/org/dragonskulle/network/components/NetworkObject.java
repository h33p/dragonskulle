/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.exceptions.NetworkObjectDoesNotHaveChildException;
import org.dragonskulle.network.ClientGameInstance;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.Capital.Capital;
import org.dragonskulle.network.components.Capital.CapitalRenderable;
import org.dragonskulle.network.components.Capital.NetworkedTransform;
import sun.misc.IOUtils;

/**
 * The type Network object.
 *
 * @author Oscar L The NetworkObject deals with any networked variables.
 */
public class NetworkObject extends GameObject {
    /** A reference to itself. */
    private final Reference<NetworkObject> mReference = new Reference<>(this);

    private static final Logger mLogger = Logger.getLogger(NetworkObject.class.getName());
    /** true if the component is on the server. */
    private final boolean isServer;
    /** The id of the object. */
    public final int networkObjectId;

    /** The counter used to assign children ids. */
    private final AtomicInteger mNetworkComponentCounter = new AtomicInteger(0);
    /** true if linked to a game scene. */
    private boolean linkedToScene;

    /**
     * Children are put to sleep when a respawn request has been made, this avoid multiple requests
     * being made before it has received its first.
     */
    private final ArrayList<Integer> sleepingChildren = new ArrayList<>();

    /** Sets that it is linked to a game scene. */
    public void linkToScene() {
        this.linkedToScene = true;
    }

    /**
     * Instantiates a new Network object.
     *
     * @param id the id
     * @param isServer true if the object is on the server
     */
    public NetworkObject(int id, boolean isServer) {
        super("network_object_" + id);
        networkObjectId = id;
        this.isServer = isServer;
    }

    /**
     * Gets a reference to the object.
     *
     * @return the reference
     */
    public Reference<NetworkObject> getNetReference() {
        return this.mReference;
    }

    /**
     * Get networkable.
     *
     * @param n the component to get its game version
     * @return the networkable retrieved. null if not found
     */
    public NetworkableComponent get(NetworkableComponent n) {
        final Reference<? extends NetworkableComponent> networkableComponentReference =
                this.getComponent(n.getClass());
        if (networkableComponentReference != null) {
            return networkableComponentReference.get();
        }
        return null;
    }

    /**
     * Finds a networkable component by id.
     *
     * @param componentId the id
     * @return the networkable, null if not found.
     */
    public Reference<NetworkableComponent> findComponent(int componentId) {
        return this.getNetworkableChildren().stream()
                .filter(e -> e.get().getId() == componentId)
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
     * Get networkable by id.
     *
     * @param id the id
     * @return the networkable, null if not found.
     */
    public Reference<NetworkableComponent> get(int id) {
        return getNetworkableChildren().stream()
                .filter(e -> e.get().getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all networkable children, if its linked to a game scene then it will get the components
     * from the scene, otherwise its local copy.
     *
     * @return the networkable children
     */
    public ArrayList<Reference<NetworkableComponent>> getNetworkableChildren() {
        ArrayList<Reference<NetworkableComponent>> networkableChildren = new ArrayList<>();
        this.getComponents(NetworkableComponent.class, networkableChildren);
        return networkableChildren;
    }

    @Override
    public String toString() {
        return "NetworkObject{"
                + "children="
                + getNetworkableChildren()
                + ", networkObjectId='"
                + networkObjectId
                + '\''
                + '}';
    }

    /**
     * Gets the object id.
     *
     * @return the id
     */
    public int getId() {
        return this.networkObjectId;
    }

    /**
     * Updates itself from bytes authored by server.
     *
     * @param payload the payload
     * @param instance the instance
     * @throws IOException thrown if failed to read client streams
     */
    public void updateFromBytes(byte[] payload, ClientGameInstance instance) throws IOException {
        int networkObjectId =
                NetworkableComponent.getComponentIdFromBytes(payload, 0); // reads 4 bytes in

        int maskLength =
                NetworkMessage.getFieldLengthFromBytes(
                        payload,
                        4); // offset of 4 to ignore id, another 4 to avoid networkObject id

        boolean[] masks = NetworkMessage.getMaskFromBytes(payload, maskLength, 4);
        ArrayList<byte[]> arrayOfChildrenBytes =
                getChildrenUpdateBytes(payload, 1 + maskLength + 4);
        int j = 0;
        for (int i = 0; i < masks.length; i++) {
            boolean shouldUpdate = masks[i];
            if (shouldUpdate) {
                try {
                    int componentId =
                            NetworkableComponent.getComponentIdFromBytes(
                                    arrayOfChildrenBytes.get(j), 0); // read 4 bytes
                    int ownerId =
                            NetworkableComponent.getComponentIdFromBytes(
                                    arrayOfChildrenBytes.get(j), 4); // re
                    mLogger.fine(
                            "Parent id of child to update is :"
                                    + ownerId
                                    + "\nComponent id of children bytes to update is : "
                                    + componentId);
                    Reference<NetworkableComponent> noc = this.findComponent(componentId);
                    mLogger.fine("Did i manage to find the component? " + (noc == null));
                    if (noc == null) {
                        throw new NetworkObjectDoesNotHaveChildException(
                                "Can't find component", componentId);
                    } else {
                        noc.get().updateFromBytes(arrayOfChildrenBytes.get(j));
                    }
                } catch (NetworkObjectDoesNotHaveChildException e) {
                    mLogger.info("NOB doesn't have child, " + e.invalidComponentId);
                    if (sleepingChildren.contains(e.invalidComponentId)) {
                        mLogger.info(
                                "Not requesting update as child is sleeping until we receive spawn request");
                    } else {
                        this.markSleepingChildUpdatesUntilSpawn(e.invalidComponentId);
                        instance.sendBytesCallback.send(
                                NetworkMessage.build(
                                        (byte) 50,
                                        NetworkMessage.convertIntToByteArray(
                                                e.invalidComponentId)));
                        break;
                    }
                }
                j++;
            } else {
                mLogger.fine("Shouldn't update child");
            }
        }
    }

    /**
     * Marks a child as sleeping until it has been respawned.
     *
     * @param invalidComponentId the child to be put to sleep.
     */
    private void markSleepingChildUpdatesUntilSpawn(int invalidComponentId) {
        mLogger.info("Marking component to sleep");
        Collections.synchronizedList(this.sleepingChildren).add(invalidComponentId);
    }

    /**
     * Removes a child from sleeping state.
     *
     * @param invalidComponentId the child id
     */
    private void removeChildFromSleepingState(int invalidComponentId) {

        List sleepers = Collections.synchronizedList(this.sleepingChildren);
        if (sleepers.contains(invalidComponentId)) {
            sleepers.remove((Object) invalidComponentId);
        }
    }

    /**
     * Seperates the updates for each children from the bytes it receives.
     *
     * @param buff the buff
     * @param offset the offset
     * @return the children update bytes
     * @throws IOException the io exception
     */
    private ArrayList<byte[]> getChildrenUpdateBytes(byte[] buff, int offset) throws IOException {
        ArrayList<byte[]> out = new ArrayList<>();
        ArrayList<Byte> objectBytes;
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        final long didSkip = bis.skip(offset); // ignores the mask length and mask bytes
        if (didSkip == offset) {
            objectBytes = new ArrayList<>();
            while (bis.available() > 0) {
                bis.mark(NetworkMessage.FIELD_SEPERATOR.length);
                byte[] nextFiveBytes =
                        IOUtils.readNBytes(bis, NetworkMessage.FIELD_SEPERATOR.length);
                bis.reset();
                if (Arrays.equals(nextFiveBytes, NetworkMessage.FIELD_SEPERATOR)) {
                    // seek field bytes
                    IOUtils.readExactlyNBytes(bis, 5);
                    // end of sync var;
                    // try to deserialize.
                    out.add(NetworkMessage.toByteArray(objectBytes));
                    objectBytes.clear(); // clears current sync bytes that have been read
                } else {
                    for (byte b : IOUtils.readNBytes(bis, 1)) {
                        objectBytes.add(b); // read one byte from stream
                    }
                }
            }
            if (!objectBytes.isEmpty()) {
                out.add(NetworkMessage.toByteArray(objectBytes));
            }
        }
        return out;
    }

    /** A callback to broadcast a message to all clients */
    public interface ServerBroadcastCallback {
        /**
         * Call.
         *
         * @param bytes the bytes
         */
        void call(byte[] bytes);
    }

    /** A callback to broadcast a message to a SINGLE clients,this client is the owner */
    public interface SendBytesToClientCallback {
        /**
         * Call.
         *
         * @param bytes the bytes
         */
        void call(byte[] bytes);
    }

    /**
     * Add a networkable child to network object
     *
     * @param child The networkable component to be added
     */
    public void addNetworkableComponent(NetworkableComponent child) {

        if (linkedToScene) {
            spawnRenderableOnGame(child); // only if !isServer
        }
        mLogger.info("Linked to scene adding component to scene");
        this.addComponent(child);
        // remove child from sleeping state if it exists
        removeChildFromSleepingState(child.getId());
        mLogger.info("my networkable components are : " + this.getNetworkableChildren().toString());
    }

    /**
     * Spawns the renderable component for the networkable on the game.
     *
     * @param child the child
     */
    private void spawnRenderableOnGame(NetworkableComponent child) {
        mLogger.info("attempting to spawn renderable for networkable component");
        Class<?> clazz = child.getClass();
        mLogger.info("attempting to spawn renderable for networkable component clazz -> " + clazz);
        if (clazz.equals(Capital.class)) {
            mLogger.info("attempting to spawn capital renderable");
            this.addComponent(CapitalRenderable.get());
        }
    }

    /**
     * Add multiple networkable children to network object
     *
     * @param children The networkable components to be added
     */
    public void addChildren(Reference<NetworkableComponent>[] children) {
        for (Reference<NetworkableComponent> child : children) {
            this.addComponent(child.get());
        }
    }

    /**
     * Spawns a component and notifies all clients using callback.
     *
     * @param component The component to be spawned, must extend NetworkableComponent
     * @param messageCode The message code of the spawn.
     * @return The ID of the spawned component
     */
    private int serverSpawnComponent(
            NetworkableComponent component,
            byte messageCode,
            ServerBroadcastCallback broadcastCallback) {
        mLogger.fine("spawning component on all clients 2");
        if (isServer) {
            component.connectSyncVars();
        }
        byte[] spawnComponentBytes;
        byte[] componentBytes = component.serializeFully();
        spawnComponentBytes = NetworkMessage.build(messageCode, componentBytes);
        broadcastCallback.call(spawnComponentBytes);
        addNetworkableComponent(component);
        return component.getId();
    }

    /**
     * Spawns the component on the server.
     *
     * @param component the component
     * @param broadcastCallback the broadcast callback
     * @return the id of the component spawned
     */
    public int serverSpawnComponent(
            NetworkableComponent component, ServerBroadcastCallback broadcastCallback) {
        mLogger.fine("spawning component on all clients 1");
        byte[] spawnComponentBytes;
        byte[] componentBytes = component.serializeFully();
        spawnComponentBytes = NetworkMessage.build((byte) 22, componentBytes);
        broadcastCallback.call(spawnComponentBytes);
        addNetworkableComponent(component);
        return component.getId();
    }

    /**
     * Spawns a capital using the @link{spawnComponent} method
     *
     * @param ownerId the owner id
     * @param broadcastCallback the broadcast callback
     * @return The id of the spawned component
     */
    public int serverSpawnCapital(int ownerId, ServerBroadcastCallback broadcastCallback) {
        Capital capital = new Capital(ownerId, this.allocateId());
        NetworkableComponent transformComponent =
                new NetworkedTransform(this.getId(), allocateId(), true);
        transformComponent.connectSyncVars();
        this.addNetworkableComponent(transformComponent);
        return serverSpawnComponent(capital, (byte) 21, broadcastCallback);
    }

    /**
     * Sends the map to the client, this is called on connect
     *
     * @param mapBytes The bytes of the serialized map.
     * @param clientCallback the client callback
     */
    public void spawnMap(byte[] mapBytes, SendBytesToClientCallback clientCallback) {
        mLogger.fine("spawning map on client");
        mLogger.fine("Map bytes :: " + mapBytes.length + "bytes");
        byte[] spawnMapMessage = NetworkMessage.build((byte) 20, mapBytes);
        clientCallback.call(spawnMapMessage);
    }

    /**
     * Children of the object will be networkable and updated on clients, only used if not spawned
     * in a game scene
     */
    private final ArrayList<Reference<NetworkableComponent>> nonLinkedToGameChildren =
            new ArrayList<>();

    /**
     * Gets network object id.
     *
     * @return the network object id
     */
    public int getNetworkObjectId() {
        return networkObjectId;
    }

    /**
     * Broadcasts updates all of the modified children as one message @param broadcastCallback the
     * broadcast callback
     *
     * @param broadcastCallback the broadcast callback
     */
    public void broadcastUpdate(ServerBroadcastCallback broadcastCallback) {
        // write 4 byte size of each child, then write child bytes.
        boolean shouldBroadcast = false;
        ArrayList<Reference<NetworkableComponent>> networkableChildren =
                this.getNetworkableChildren();
        boolean[] didChildUpdateMask = new boolean[networkableChildren.size()];
        mLogger.info("Networkable Object has n children : " + networkableChildren.size());
        for (int i = 0; i < didChildUpdateMask.length; i++) {
            if (networkableChildren.get(i).get().hasBeenModified()) {
                mLogger.info("child has been modified in a networkobject");
                didChildUpdateMask[i] = true;
                if (!shouldBroadcast) {
                    shouldBroadcast = true;
                }
            }
        }
        if (shouldBroadcast) {
            byte[] bytes = generateBroadcastUpdateBytes(didChildUpdateMask);
            mLogger.fine("Broadcast update size:: " + bytes.length);
            broadcastCallback.call(NetworkMessage.build((byte) 15, bytes));
        }
    }

    /**
     * Generates the updates for all of its children which changed.
     *
     * @param didChildUpdateMask the mask of children which updates
     * @return the bytes to be broadcasted
     */
    private byte[] generateBroadcastUpdateBytes(boolean[] didChildUpdateMask) {
        //        mLogger.fine("generating broadcast update bytes");
        ArrayList<Byte> bytes = new ArrayList<>();

        byte[] idBytes = NetworkMessage.convertIntToByteArray(this.getNetworkObjectId()); // 4
        byte sizeOfMaskBytes = (byte) didChildUpdateMask.length; // 1

        ArrayList<Byte> childChunk = new ArrayList<>();
        ArrayList<Byte> contents = new ArrayList<>();
        ArrayList<Reference<NetworkableComponent>> networkableChildren =
                this.getNetworkableChildren();

        for (int i = 0; i < didChildUpdateMask.length; i++) {
            if (didChildUpdateMask[i]) {
                // child did update
                byte[] childBytes = networkableChildren.get(i).get().serialize();
                for (byte childByte : childBytes) {
                    childChunk.add(childByte);
                }

                if (i < didChildUpdateMask.length - 1) {
                    for (byte b : NetworkMessage.FIELD_SEPERATOR) {
                        childChunk.add(b);
                    }
                }

                contents.addAll(childChunk);
            }
        }

        // add id
        for (byte idByte : idBytes) {
            bytes.add(idByte);
        }

        // add size of mask only one byte
        bytes.add(sizeOfMaskBytes);

        // add mask
        byte[] maskBytes = new byte[didChildUpdateMask.length];
        for (int i = 0; i < didChildUpdateMask.length; i++) {
            boolean didChildUpdate = didChildUpdateMask[i];
            if (didChildUpdate) {
                maskBytes[i] = ((byte) 1);
                bytes.add((byte) 1);
            } else {
                maskBytes[i] = ((byte) 0);
                bytes.add((byte) 0);
            }
        }

        // add contents
        bytes.addAll(contents);
        return NetworkMessage.toByteArray(bytes);
    }

    /**
     * Allocates an id in the form child id + parentId.
     *
     * @return the int
     */
    private int allocateId() {
        StringBuilder sb = new StringBuilder();
        int componentId = this.mNetworkComponentCounter.incrementAndGet();
        sb.append(componentId);
        sb.append(this.networkObjectId);
        return Integer.parseInt(sb.toString());
    }

    /**
     * Gets id of the object from the bytes.
     *
     * @param payload the payload
     * @return the id from bytes
     */
    public static int getIdFromBytes(byte[] payload) {
        return NetworkMessage.convertByteArrayToInt(Arrays.copyOf(payload, 4));
    }
}
