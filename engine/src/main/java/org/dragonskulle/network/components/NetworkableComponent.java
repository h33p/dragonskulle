/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.ISyncVar;

/**
 * @author Oscar L Any component that extends this, its syncvars will be updated with the server.
 */
@Accessors(prefix = "m")
@Log
public abstract class NetworkableComponent extends Component {

    /** A reference to itself. */
    private final Reference<NetworkableComponent> mReference = new Reference<>(this);

    @Getter private NetworkObject mNetworkObject = null;

    public static final int MASK_LENGTH_OFFSET = 0;
    public static final int MASK_OFFSET = MASK_LENGTH_OFFSET + 1;

    /** Instantiates a new Networkable component. */
    public NetworkableComponent() {}

    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */
    private boolean[] mFieldsMask;

    /** The Fields. */
    private Field[] mFields;

    /**
     * Init fields. @param networkObject the network object
     *
     * @param outRequests the requests it can deal with
     */
    public void initialize(NetworkObject networkObject, List<ClientRequest<?>> outRequests) {

        mNetworkObject = networkObject;

        onNetworkInitialize();

        mFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ISyncVar.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);

        for (Field f : mFields) {
            f.setAccessible(true);
        }

        Field[] requestFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ClientRequest.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);

        try {
            for (Field f : requestFields) {
                f.setAccessible(true);
                ClientRequest<?> req = (ClientRequest<?>) f.get(this);
                outRequests.add(req);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (networkObject.isServer()) connectSyncVars();

        onConnectedSyncvars();
    }

    /** Connect sync vars. Only should be ran on server */
    private void connectSyncVars() {
        log.info("Connecting sync vars for component");
        mFieldsMask = new boolean[mFields.length];
        int i = 0;
        for (Field f : mFields) {
            try {
                ISyncVar sv =
                        (ISyncVar) f.get(this); // retrieves syncvar from child and adds it to the
                // connection array
                int finalI = i;
                sv.registerListener(() -> this.handleFieldChange(finalI));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    protected void onNetworkInitialize() {}

    protected void onConnectedSyncvars() {}

    void beforeNetSerialize() {}

    protected void afterNetUpdate() {}

    /**
     * Reset the changed bitmask
     *
     * <p>Call this after every network update, once all clients had their state updated
     */
    void resetUpdateMask() {
        for (int i = 0; i < mFieldsMask.length; i++) mFieldsMask[i] = false;
    }

    /**
     * Serialize component.
     *
     * @param stream the stream to write to
     * @param force whether to write all fields in
     */
    public void serialize(DataOutputStream stream, boolean force) throws IOException {
        int maskLength = this.mFields.length; // 1byte
        boolean[] mask = new boolean[maskLength];

        for (int i = 0; i < mFields.length; i++) {
            mask[i] = force || mFieldsMask[i];
        }

        byte[] byteMask = NetworkMessage.convertBoolArrayToBytes(mask);

        stream.writeByte(byteMask.length);

        for (byte b : byteMask) stream.writeByte(b);

        for (int i = 0; i < this.mFields.length; i++) {
            Field f = this.mFields[i];
            if (mask[i]) {
                try {
                    ((ISyncVar) f.get(this)).serialize(stream);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Serialize component.
     *
     * @param stream the stream to write to
     */
    public void serialize(DataOutputStream stream) throws IOException {
        serialize(stream, false);
    }

    /**
     * Serialize fully byte, is this ran on spawn when the whole component needs creating.
     *
     * @param stream the stream to write to
     */
    public void serializeFully(DataOutputStream stream) throws IOException {
        serialize(stream, true);
    }

    /**
     * Update fields from a stream.
     *
     * @param stream stream containing the payload
     * @throws IOException the io exception
     */
    public void updateFromStream(DataInputStream stream) throws IOException {
        int maskLength = stream.readByte();
        // cast to one byte
        boolean[] mask = NetworkMessage.getMaskFromBytes(stream.readNBytes(maskLength));

        for (int i = 0; i < mask.length && i < mFields.length; i++) {
            if (mask[i]) {
                try {
                    Field field = this.mFields[i];
                    ISyncVar obj = (ISyncVar) field.get(this);
                    obj.deserialize(stream);
                } catch (Exception e) {
                    log.fine("Failed to deserialize " + this.mFields[i].getName());
                    e.printStackTrace();
                }
            }
        }

        afterNetUpdate();
    }

    /**
     * Handle field change, sets mask to true when field has been edited.
     *
     * @param maskId the mask id
     */
    private void handleFieldChange(int maskId) {
        this.mFieldsMask[maskId] = true;
    }

    /**
     * Has been field been modified?.
     *
     * @return the boolean
     */
    public boolean hasBeenModified() {
        boolean hasTrueInMask = false;
        if (mFields == null) {
            log.info("mFields is not set yet, the component hasn't connected");
        } else {
            for (boolean b : mFieldsMask) {
                if (b) {
                    hasTrueInMask = true;
                    break;
                }
            }
        }
        return hasTrueInMask;
    }

    @Override
    public String toString() {
        StringBuilder fieldsString = new StringBuilder("Field{\n");
        for (Field field : mFields) {
            try {
                fieldsString.append(field.get(this).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        fieldsString.append("\n}");

        return "NetworkableComponent{"
                + "fieldsMask="
                + Arrays.toString(mFieldsMask)
                + ", fields="
                + fieldsString
                + '}';
    }

    /**
     * Gets a reference to this component.
     *
     * @return the reference
     */
    public Reference<NetworkableComponent> getNetReference() {
        return this.mReference;
    }

    /**
     * Get the {@link NetworkObject}'s {@link NetworkManager}, if the NetworkObject exists.
     *
     * @return The NetworkManager, or {@code null}.
     */
    public NetworkManager getNetworkManager() {
        if (mNetworkObject == null) return null;
        return mNetworkObject.getNetworkManager();
    }
}
