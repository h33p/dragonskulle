/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
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
import org.dragonskulle.network.components.sync.ISyncVar;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.utils.IOUtils;

/**
 * The playerStyle Network object.
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

    private final ArrayList<Boolean> mTmpMask = new ArrayList<>();
    private final ArrayList<ISyncVar> mTmpVar = new ArrayList<>();

    private final ServerEvent<NoneData> mDestroyEvent =
            new ServerEvent<>(
                    NoneData.DATA,
                    (__) -> {
                        mDestroyed = true;
                        getGameObject().destroy();
                    },
                    EventRecipients.ACTIVE_CLIENTS,
                    EventTimeframe.LONG_TERM_DELAYABLE);

    private final ServerEvent<SyncInt> mOwnerIdChangeEvent =
            new ServerEvent<>(
                    new SyncInt(),
                    (data) -> checkedOwnerIdSet(data.get()),
                    EventRecipients.ACTIVE_CLIENTS,
                    EventTimeframe.LONG_TERM_DELAYABLE);

    /** The network client ID that owns this. */
    @Getter private int mOwnerId;

    /**
     * Instantiates a new Network object.
     *
     * @param id the id
     * @param ownerId the id of the owner
     * @param isServer true if the object is on the server
     * @param manager network manager controlling this object
     */
    public NetworkObject(int id, int ownerId, boolean isServer, NetworkManager manager) {
        mNetworkObjectId = id;
        mOwnerId = ownerId;
        mIsServer = isServer;
        mNetworkManager = manager;
    }

    /**
     * Check whether this object is owned by the client/server.
     *
     * @return {@code true} if the object is ours, {@code false} otherwise.
     */
    public boolean isMine() {
        if (mOwnerId < 0 && mIsServer) {
            return true;
        }

        ClientNetworkManager clientManager = mNetworkManager.getClientManager();
        return clientManager != null && clientManager.getNetId() == mOwnerId;
    }

    /**
     * Sets the owner ID of the object.
     *
     * <p>This method only has effect on the server. It sets the owner ID of the object, and
     * broadcasts this change to the clients.
     *
     * @param newOwnerID new owner ID to set
     */
    public void setOwnerId(int newOwnerID) {
        if (mIsServer) {
            checkedOwnerIdSet(newOwnerID);
        }
    }

    /**
     * Set new owner ID, and broadcast change events if the ID differs
     *
     * <p>This method will dispatch {@link NetworkableComponent#onOwnerIdChange} event on all
     * networkable components, and invoke owner ID change event on remote clients, if running as
     * server.
     *
     * @param newOwnerId new ID to set
     */
    private void checkedOwnerIdSet(int newOwnerId) {
        if (newOwnerId != mOwnerId) {
            for (Reference<NetworkableComponent> netComp : mNetworkableComponents) {
                if (Reference.isValid(netComp)) {
                    netComp.get().onOwnerIdChange(newOwnerId);
                }
            }

            mOwnerId = newOwnerId;

            if (mIsServer) {
                mOwnerIdChangeEvent.invoke((d) -> d.set(mOwnerId));
            }
        }
    }

    @Override
    public void onDestroy() {
        if (!mDestroyed && mIsServer) {
            mDestroyEvent.invoke(NoneData.DATA);
        }
    }

    /**
     * Initialize the network object.
     *
     * <p>This method will initialize all networkable components, attach syncvars, events, and
     * requests to be ready for updates.
     */
    void networkInitialize() {
        getGameObject().getComponents(NetworkableComponent.class, mNetworkableComponents);

        mServerEvents.add(mDestroyEvent);
        mServerEvents.add(mOwnerIdChangeEvent);

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
     * Called before the network object is serialized.
     *
     * <p>This method will invoke {@link NetworkableComponent#beforeNetSerialize()} method on all
     * netowkrable components on the object.
     */
    void beforeNetSerialize() {
        for (Reference<NetworkableComponent> comp : mNetworkableComponents) {
            NetworkableComponent nc = comp.get();
            nc.beforeNetSerialize();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
        if (requestID < 0 || requestID >= mClientRequests.size()) {
            return false;
        }

        mClientRequests.get(requestID).handle(stream);

        return true;
    }

    /**
     * Handle a server event.
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
        if (eventID < 0 || eventID >= mServerEvents.size()) {
            return false;
        }

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
     */
    public void updateFromBytes(DataInputStream stream) throws IOException {
        int maskLength = stream.readByte();

        byte[] mask = IOUtils.readNBytes(stream, maskLength);

        boolean[] masks = NetworkMessage.getMaskFromBytes(mask);

        mTmpVar.clear();

        mNetworkableComponents.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .flatMap(NetworkableComponent::getSyncVars)
                .collect(Collectors.toCollection(() -> mTmpVar));

        int sz = mTmpVar.size();

        for (int i = 0; i < sz; i++) {
            if (!masks[i]) continue;
            mTmpVar.get(i).deserialize(stream);
        }

        mNetworkableComponents.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(NetworkableComponent::afterNetUpdate);
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
                .flatMap(NetworkableComponent::getSyncVars)
                .forEach(ISyncVar::resetDirtyFlag);
    }

    /**
     * Broadcasts updates all of the modified children as one message @param broadcastCallback the
     * broadcast callback.
     *
     * @param client client to update the values for
     * @param forceUpdate whether or not forcefully update all syncvars
     */
    public void sendUpdate(ServerClient client, boolean forceUpdate) {
        int netId = client.getNetworkID();

        mTmpVar.clear();

        mNetworkableComponents.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .flatMap(NetworkableComponent::getSyncVars)
                .collect(Collectors.toCollection(() -> mTmpVar));

        mTmpMask.clear();

        mTmpVar.stream()
                .map(s -> forceUpdate || s.isDirty(netId))
                .collect(Collectors.toCollection(() -> mTmpMask));

        boolean shouldBroadcast = mTmpMask.contains(true);

        if (!shouldBroadcast) {
            return;
        }

        byte[] mask = NetworkMessage.convertCollectionMaskToBytes(mTmpMask);

        try (DataOutputStream stream = client.getDataOut()) {
            stream.writeByte(NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT);
            stream.writeInt(getNetworkObjectId());
            stream.writeByte((byte) mask.length);
            stream.write(mask);

            int sz = mTmpVar.size();

            for (int i = 0; i < sz; i++) {
                if (!mTmpMask.get(i)) continue;
                mTmpVar.get(i).serialize(stream, netId);
                mTmpVar.get(i).resetDirtyFlag(netId);
            }
        } catch (IOException e) {
            log.warning("Failed to serialize data!");
            e.printStackTrace();
        }
    }
}
