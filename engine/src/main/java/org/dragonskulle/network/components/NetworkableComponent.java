/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.ISyncVar;

/**
 * @author Oscar L Any component that extends this, its syncvars will be updated with the server.
 */
@Accessors(prefix = "m")
public abstract class NetworkableComponent extends Component {
    private static final Logger mLogger = Logger.getLogger(NetworkableComponent.class.getName());
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
    }

    /** Connect sync vars. Only should be ran on server */
    private void connectSyncVars() {
        mLogger.info("Connecting sync vars for component");
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
     * @param force whether to write all fields in
     * @return the bytes of the component
     */
    public byte[] serialize(boolean force) {
        int maskLength = this.mFields.length; // 1byte
        ArrayList<Byte> mask = new ArrayList<>(maskLength);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            DataOutputStream stream = new DataOutputStream(bos);
            for (int i = 0; i < this.mFields.length; i++) {
                boolean didVarChange = this.mFieldsMask[i];
                Field f = this.mFields[i];
                if (didVarChange || force) {
                    try {
                        mask.add((byte) 1);
                        ((ISyncVar) f.get(this)).serialize(stream);
                    } catch (IllegalAccessException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    mask.add((byte) 0);
                }
            }
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) maskLength);
        payload.addAll(mask);

        for (byte b : bos.toByteArray()) {
            payload.add(b);
        }

        return NetworkMessage.toByteArray(payload);
    }

    /**
     * Serialize component.
     *
     * @return the bytes of the component
     */
    public byte[] serialize() {
        return serialize(false);
    }

    /**
     * Serialize fully byte, is this ran on spawn when the whole component needs creating.
     *
     * @return the bytes
     */
    public byte[] serializeFully() {
        return serialize(true);
    }

    /**
     * Update fields from bytes.
     *
     * @param payload the payload
     * @throws IOException the io exception
     */
    public void updateFromBytes(byte[] payload) throws IOException {
        int maskLength = NetworkMessage.getFieldLengthFromBytes(payload, MASK_LENGTH_OFFSET);
        // cast to one byte
        boolean[] masks = NetworkMessage.getMaskFromBytes(payload, maskLength, MASK_OFFSET);
        updateSyncVarsFromBytes(masks, payload, MASK_OFFSET + maskLength);
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
            mLogger.info("mFields is not set yet, the component hasn't connected");
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
     * Gets contents from bytes.
     *
     * @param mask the mask
     * @param buff the buff
     * @param offset the offset
     * @throws IOException the io exception
     */
    private void updateSyncVarsFromBytes(boolean[] mask, byte[] buff, int offset)
            throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        final long didSkip = bis.skip(offset);

        DataInputStream stream = new DataInputStream(bis);

        if (didSkip != offset) return;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                try {
                    Field field = this.mFields[i];
                    ISyncVar obj = (ISyncVar) field.get(this);
                    obj.deserialize(stream);
                } catch (Exception e) {
                    mLogger.fine("Failed to deserialize " + this.mFields[i].getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets a reference to this component.
     *
     * @return the reference
     */
    public Reference<NetworkableComponent> getNetReference() {
        return this.mReference;
    }
}
