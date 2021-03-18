/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.utils.IOUtils;

/**
 * The type Network object.
 *
 * @author Oscar L The NetworkObject deals with any networked variables.
 */
@Accessors(prefix = "m")
public class NetworkObject extends Component implements IOnAwake {

    private static final Logger mLogger = Logger.getLogger(NetworkObject.class.getName());
    /** true if the component is on the server. */
    @Getter private final boolean mIsServer;
    /** The id of the object. */
    public final int mNetworkObjectId;

    @Getter
    private final ArrayList<Reference<NetworkableComponent>> mNetworkableComponents =
            new ArrayList<>();

    @Getter private final ArrayList<ClientRequest<?>> mClientRequests = new ArrayList<>();

    /** The network client ID that owns this */
    @Getter private int mOwnerId;

    /**
     * Instantiates a new Network object.
     *
     * @param id the id
     * @param ownerId the id of the owner
     * @param isServer true if the object is on the server
     */
    public NetworkObject(int id, int ownerId, boolean isServer) {
        mNetworkObjectId = id;
        mOwnerId = ownerId;
        mIsServer = isServer;
    }

    /**
     * Check whether this object is owned by the client/server
     *
     * @return {@code true} if the object is ours, {@code false} otherwise.
     */
    public boolean isMine() {
        if (mOwnerId < 0 && mIsServer) return true;

        NetworkManager networkManager = Scene.getActiveScene().getSingleton(NetworkManager.class);

        if (networkManager == null) return false;

        ClientNetworkManager clientManager = networkManager.getClientManager();

        return clientManager != null && clientManager.getNetID() == mOwnerId;
    }

    public void setOwnerId(int newOwnerID) {
        if (mIsServer) mOwnerId = newOwnerID;
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onAwake() {
        getGameObject().getComponents(NetworkableComponent.class, mNetworkableComponents);

        for (Reference<NetworkableComponent> comp : mNetworkableComponents) {
            NetworkableComponent nc = comp.get();
            nc.initialize(this, mClientRequests);
        }

        int id = 0;
        for (ClientRequest<?> req : mClientRequests) {
            req.attachNetworkObject(this, id++);
        }
    }

    /**
     * Gets id from bytes.
     *
     * @param payload the payload
     * @param offset the offset
     * @return the id from bytes
     */
    public static int getIntFromBytes(byte[] payload, int offset) {
        byte[] bytes = Arrays.copyOfRange(payload, offset, offset + 4);

        return NetworkMessage.convertByteArrayToInt(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkObject that = (NetworkObject) o;
        return Objects.equals(mNetworkObjectId, that.mNetworkObjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNetworkObjectId);
    }

    @Override
    public String toString() {
        return "NetworkObject{"
                + "children="
                + getNetworkableComponents()
                + ", networkObjectId='"
                + mNetworkObjectId
                + '\''
                + '}';
    }

    /**
     * Gets the object id.
     *
     * @return the id
     */
    public int getId() {
        return this.mNetworkObjectId;
    }

    /**
     * Handle a client request
     *
     * <p>This will take a client's request and handle it.
     *
     * <p>The dataflow looks like so:
     *
     * <p>ClientRequest::invoke to Server to here to ClientRequest::handle
     *
     * @param requestID the request id
     * @param stream the input stream
     * @return true if executed successfully.
     * @throws IOException the io exception
     */
    public boolean handleClientRequest(int requestID, DataInputStream stream) throws IOException {
        if (requestID < 0 || requestID >= mClientRequests.size()) return false;

        mClientRequests.get(requestID).handle(stream);

        return true;
    }

    public static final int ID_OFFSET = 0;
    public static final int OWNER_ID_OFFSET = ID_OFFSET + 4;
    public static final int MASK_LENGTH_OFFSET = OWNER_ID_OFFSET + 4;
    public static final int MASK_OFFSET = MASK_LENGTH_OFFSET + 1;

    /**
     * Updates itself from bytes authored by server.
     *
     * @param payload the payload
     * @throws IOException thrown if failed to read client streams
     */
    public void updateFromBytes(byte[] payload) throws IOException {
        // TODO clear this up by using ByteStreams
        int networkObjectId = getIntFromBytes(payload, ID_OFFSET);

        int ownerId = getIntFromBytes(payload, OWNER_ID_OFFSET);
        mOwnerId = ownerId;

        int maskLength = NetworkMessage.getFieldLengthFromBytes(payload, MASK_LENGTH_OFFSET);

        boolean[] masks = NetworkMessage.getMaskFromBytes(payload, maskLength, MASK_OFFSET);
        ArrayList<byte[]> arrayOfChildrenBytes =
                getChildrenUpdateBytes(payload, MASK_OFFSET + maskLength);
        int j = 0;
        final int mNetworkableComponentsSize = mNetworkableComponents.size();
        for (int i = 0; i < masks.length; i++) {
            boolean shouldUpdate = masks[i];
            if (shouldUpdate) {
                mLogger.fine(
                        "Parent id of child to update is :"
                                + ownerId
                                + "\nComponent id of children bytes to update is : "
                                + i);
                if (i < mNetworkableComponentsSize) {
                    NetworkableComponent noc = mNetworkableComponents.get(i).get();
                    mLogger.fine("Did i manage to find the component? " + (noc == null));
                    if (noc == null) {
                        throw new IOException(String.format("Can't find component %d", i));
                    } else {
                        noc.updateFromBytes(arrayOfChildrenBytes.get(j));
                    }
                }
                j++;
            } else {
                mLogger.fine("Shouldn't update child");
            }
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

    /**
     * Gets network object id.
     *
     * @return the network object id
     */
    public int getNetworkObjectId() {
        return mNetworkObjectId;
    }

    /**
     * Reset update mask
     *
     * <p>Called after all clients got their updates sent.
     */
    public void resetUpdateMask() {
        mNetworkableComponents.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(NetworkableComponent::resetUpdateMask);
    }

    /**
     * Broadcasts updates all of the modified children as one message @param broadcastCallback the
     * broadcast callback
     *
     * @param client client to update the values for
     * @param forceUpdate whether or not forcefully update all syncvars
     */
    public void sendUpdate(ServerClient client, boolean forceUpdate) {
        // write 4 byte size of each child, then write child bytes.
        boolean shouldBroadcast = false;
        boolean[] didChildUpdateMask = new boolean[mNetworkableComponents.size()];
        mLogger.fine("Networkable Object has n components : " + mNetworkableComponents.size());
        for (int i = 0; i < didChildUpdateMask.length; i++) {
            if (forceUpdate || mNetworkableComponents.get(i).get().hasBeenModified()) {
                didChildUpdateMask[i] = true;
                if (!shouldBroadcast) {
                    shouldBroadcast = true;
                }
            }
        }
        if (shouldBroadcast) {
            byte[] bytes = generateUpdateBytes(didChildUpdateMask);
            mLogger.fine(
                    "Update update size for client "
                            + client.getNetworkID()
                            + ":: "
                            + bytes.length);
            client.sendBytes(
                    NetworkMessage.build(NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT, bytes));
        }
    }

    /**
     * Generates the updates for all of its children which changed.
     *
     * @param didChildUpdateMask the mask of children which updates
     * @return the bytes to be broadcasted
     */
    private byte[] generateUpdateBytes(boolean[] didChildUpdateMask) {
        //        mLogger.fine("generating broadcast update bytes");
        ArrayList<Byte> bytes = new ArrayList<>();

        byte[] idBytes = NetworkMessage.convertIntToByteArray(this.getNetworkObjectId()); // 4
        byte[] ownerIdBytes = NetworkMessage.convertIntToByteArray(this.getOwnerId()); // 4
        byte sizeOfMaskBytes = (byte) didChildUpdateMask.length; // 1

        ArrayList<Byte> childChunk = new ArrayList<>();
        ArrayList<Byte> contents = new ArrayList<>();

        for (int i = 0; i < didChildUpdateMask.length; i++) {
            if (didChildUpdateMask[i]) {
                // child did update
                byte[] childBytes = mNetworkableComponents.get(i).get().serialize();
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

        // add owner id
        for (byte idByte : ownerIdBytes) {
            bytes.add(idByte);
        }

        // add size of mask only one byte
        bytes.add(sizeOfMaskBytes);

        // add mask
        for (int i = 0; i < didChildUpdateMask.length; i++) {
            boolean didChildUpdate = didChildUpdateMask[i];
            if (didChildUpdate) {
                bytes.add((byte) 1);
            } else {
                bytes.add((byte) 0);
            }
        }

        // add contents
        bytes.addAll(contents);
        return NetworkMessage.toByteArray(bytes);
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
