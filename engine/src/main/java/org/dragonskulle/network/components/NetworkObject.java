/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.requests.NoneData;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.requests.ServerEvent.EventRecipients;
import org.dragonskulle.network.components.requests.ServerEvent.EventTimeframe;
import org.dragonskulle.utils.IOUtils;

/**
 * The type Network object.
 *
 * @author Oscar L The NetworkObject deals with any networked variables.
 */
@Accessors(prefix = "m")
@Log
public class NetworkObject extends Component {

    /** true if the component is on the server. */
    @Getter private final boolean mIsServer;
    /** The id of the object. */
    public final int mNetworkObjectId;

    @Getter private final NetworkManager mNetworkManager;

    @Getter
    private final ArrayList<Reference<NetworkableComponent>> mNetworkableComponents =
            new ArrayList<>();

    @Getter private final ArrayList<ClientRequest<?>> mClientRequests = new ArrayList<>();
    @Getter private final ArrayList<ServerEvent<?>> mServerEvents = new ArrayList<>();

    private boolean mDestroyed = false;

    @Getter
    private final ServerEvent<NoneData> mDestroyEvent =
            new ServerEvent<>(
                    NoneData.DATA,
                    (__) -> {
                        mDestroyed = true;
                        getGameObject().destroy();
                    },
                    EventRecipients.ACTIVE_CLIENTS,
                    EventTimeframe.LONG_TERM_DELAYABLE);

    /** The network client ID that owns this */
    @Getter private int mOwnerId;

    /**
     * Instantiates a new Network object.
     *
     * @param id the id
     * @param ownerId the id of the owner
     * @param isServer true if the object is on the server
     */
    public NetworkObject(int id, int ownerId, boolean isServer, NetworkManager manager) {
        mNetworkObjectId = id;
        mOwnerId = ownerId;
        mIsServer = isServer;
        mNetworkManager = manager;
    }

    /**
     * Check whether this object is owned by the client/server
     *
     * @return {@code true} if the object is ours, {@code false} otherwise.
     */
    public boolean isMine() {
        if (mOwnerId < 0 && mIsServer) return true;

        ClientNetworkManager clientManager = mNetworkManager.getClientManager();
        return clientManager != null && clientManager.getNetID() == mOwnerId;
    }

    public void setOwnerId(int newOwnerID) {
        if (mIsServer) mOwnerId = newOwnerID;
    }

    @Override
    public void onDestroy() {
        if (!mDestroyed && mIsServer) mDestroyEvent.invoke(NoneData.DATA);
    }

    public void networkInitialize() {
        getGameObject().getComponents(NetworkableComponent.class, mNetworkableComponents);

        mServerEvents.add(mDestroyEvent);

        for (Reference<NetworkableComponent> comp : mNetworkableComponents) {
            NetworkableComponent nc = comp.get();
            nc.initialize(this, mClientRequests, mServerEvents);
        }

        int id = 0;
        for (ClientRequest<?> req : mClientRequests) {
            req.attachNetworkObject(this, id++);
        }

        id = 0;
        for (ServerEvent<?> event : mServerEvents) {
            event.attachNetworkObject(this, id++);
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

    public void beforeNetSerialize() {
        for (Reference<NetworkableComponent> comp : mNetworkableComponents) {
            NetworkableComponent nc = comp.get();
            nc.beforeNetSerialize();
        }
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
        return "(NetworkObject{"
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

    /**
     * Handle a server event
     *
     * <p>This will take a server's event and handle it
     *
     * <p>The dataflow looks like so:
     *
     * <p>ServerEvent::invoke on the server, goes to the client, here, and then to
     * ServerEvent::handle
     *
     * @param eventID the event ID
     * @param stream the input stream
     * @return true if executed successfully.
     * @throws IOException if parsing fails
     */
    public boolean handleServerEvent(int eventID, DataInputStream stream) throws IOException {
        if (eventID < 0 || eventID >= mServerEvents.size()) return false;

        mServerEvents.get(eventID).handle(stream);

        return true;
    }

    public static final int ID_OFFSET = 0;
    public static final int OWNER_ID_OFFSET = ID_OFFSET + 4;
    public static final int MASK_LENGTH_OFFSET = OWNER_ID_OFFSET + 4;
    public static final int MASK_OFFSET = MASK_LENGTH_OFFSET + 1;

    /**
     * Updates itself from stream authored by server.
     *
     * @param stream the stream containing the payload
     * @throws IOException thrown if failed to read client streams
     * @return the owner id of the network object
     */
    public int updateFromBytes(DataInputStream stream) throws IOException {
        int ownerId = stream.readInt();
        mOwnerId = ownerId;

        int maskLength = stream.readByte();

        byte[] mask = IOUtils.readNBytes(stream, maskLength);

        boolean[] masks = NetworkMessage.getMaskFromBytes(mask);

        final int mNetworkableComponentsSize = mNetworkableComponents.size();
        for (int i = 0; i < masks.length; i++) {
            boolean shouldUpdate = masks[i];
            if (shouldUpdate) {
                log.fine(
                        "Parent id of child to update is :"
                                + ownerId
                                + "\nComponent id of children bytes to update is : "
                                + i);
                if (i < mNetworkableComponentsSize) {
                    NetworkableComponent noc = mNetworkableComponents.get(i).get();
                    log.fine("Did i manage to find the component? " + (noc == null));
                    if (noc == null) {
                        throw new IOException(String.format("Can't find component %d", i));
                    } else {
                        noc.updateFromStream(stream);
                    }
                }
            } else {
                log.fine("Shouldn't update child");
            }
        }
        return mOwnerId;
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
        log.fine("Networkable Object has n components : " + mNetworkableComponents.size());
        for (int i = 0; i < didChildUpdateMask.length; i++) {
            if (forceUpdate || mNetworkableComponents.get(i).get().hasBeenModified()) {
                didChildUpdateMask[i] = true;
                if (!shouldBroadcast) {
                    shouldBroadcast = true;
                }
            }
        }
        if (shouldBroadcast) {
            try {
                DataOutputStream stream = client.getDataOut();
                stream.writeByte(NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT);
                generateUpdateBytes(stream, didChildUpdateMask, forceUpdate);
                stream.flush();
            } catch (IOException e) {
                log.warning("Failed to serialize data!");
                e.printStackTrace();
                client.closeSocket();
            }
        }
    }

    /**
     * Generates the updates for all of its children which changed.
     *
     * @param stream stream to write to
     * @param didChildUpdateMask the mask of children which updates
     * @param forceUpdate whether to force update all components
     * @return the bytes to be broadcasted
     */
    private void generateUpdateBytes(
            DataOutputStream stream, boolean[] didChildUpdateMask, boolean forceUpdate)
            throws IOException {
        //        log.fine("generating broadcast update bytes");
        boolean[] mask = new boolean[didChildUpdateMask.length];

        for (int i = 0; i < didChildUpdateMask.length; i++) {
            mask[i] = forceUpdate || didChildUpdateMask[i];
        }

        stream.writeInt(getNetworkObjectId());
        stream.writeInt(getOwnerId());

        byte[] byteMask = NetworkMessage.convertBoolArrayToBytes(mask);

        stream.writeByte(byteMask.length);

        for (byte b : byteMask) stream.writeByte(b);

        for (int i = 0; i < didChildUpdateMask.length; i++) {
            if (didChildUpdateMask[i]) {
                mNetworkableComponents.get(i).get().serialize(stream, forceUpdate);
            }
        }
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
