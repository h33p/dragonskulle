/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import org.dragonskulle.components.Component;
import org.dragonskulle.network.DecodingException;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.sync.ISyncVar;
import org.jetbrains.annotations.NotNull;

/**
 * @param <T> the type parameter
 * @author Oscar L Any component that extends this, its syncvars will be updated with the server.
 */
public abstract class NetworkableComponent<T> extends Component {

    NetworkableComponent(int ownerId, int networkComponentId) {
        this.id = networkComponentId;
        this.ownerId = ownerId;
    }

    NetworkableComponent() {}

    /**
     * Gets id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Sets owner id.
     *
     * @param id the id
     */
    public void setOwnerId(int id) {
        this.ownerId = id;
    }

    /** The Id. */
    private int id;

    /** The network object that owns this */
    private int ownerId;
    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */
    private boolean[] mFieldsMask;

    /** The Fields. */
    private Field[] mFields;

    /** Init fields. */
    public void initFields() {
        mFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ISyncVar.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);
    }

    /** Connect sync vars. */
    public void connectSyncVars() {
        mFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ISyncVar.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);
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

    /**
     * Serialize component.
     *
     * @param force whether to write all fields in
     * @return the bytes of the component
     */
    public byte[] serialize(boolean force) {
        ArrayList<Byte> componentIdBytes = getComponentIdBytes();
        ArrayList<Byte> ownerIdBytes = getOwnerIdBytes();

        int maskLength = this.mFields.length; // 1byte
        ArrayList<Byte> mask = new ArrayList<>(maskLength);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            for (int i = 0; i < this.mFields.length; i++) {
                boolean didVarChange = this.mFieldsMask[i];
                Field f = this.mFields[i];
                if (didVarChange || force) {
                    try {
                        mask.add((byte) 1);
                        ((ISyncVar) f.get(this)).serialize(oos);
                        this.mFieldsMask[i] = false; // reset flag
                    } catch (IllegalAccessException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    mask.add((byte) 0);
                }
            }
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Byte> payload = new ArrayList<>();
        payload.addAll(componentIdBytes);
        payload.addAll(ownerIdBytes);
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
     * Gets the bytes of the id.
     *
     * @return the bytes
     */
    @NotNull
    private ArrayList<Byte> getComponentIdBytes() {
        ArrayList<Byte> componentId = new ArrayList<>(); // 4 bytes
        byte[] bytes = NetworkMessage.convertIntToByteArray(this.getId());
        for (Byte aByte : bytes) {
            componentId.add(aByte);
        }
        return componentId;
    }

    /**
     * Gets the bytes of the network object id. (OWNER)
     *
     * @return the bytes
     */
    @NotNull
    private ArrayList<Byte> getOwnerIdBytes() {
        ArrayList<Byte> ownerId = new ArrayList<>(); // 4 bytes
        byte[] bytes = NetworkMessage.convertIntToByteArray(this.getOwnerId());
        for (Byte aByte : bytes) {
            ownerId.add(aByte);
        }
        return ownerId;
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
     * Creates a networkable from the bytes.
     *
     * @param <T> the type parameter
     * @param target the target
     * @param bytes the bytes
     * @return the component
     * @throws DecodingException thrown if error in decoding
     */
    public static <T extends NetworkableComponent> T from(Class<T> target, byte[] bytes)
            throws DecodingException {
        try {
            T t = target.newInstance();
            t.initFields();
            t.updateFromBytes(bytes);
            return t;
        } catch (IOException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new DecodingException("Error decoding component from bytes");
        }
    }

    /**
     * Update fields from bytes.
     *
     * @param payload the payload
     * @throws IOException the io exception
     */
    public void updateFromBytes(byte[] payload) throws IOException {
        int id = getComponentIdFromBytes(payload, 0); // read 4 bytes
        int ownerId =
                getComponentIdFromBytes(
                        payload, 4); // read 4 bytes but with offset of 4 to avoid previous id
        this.setId(id);
        this.setOwnerId(ownerId);
        int maskLength =
                NetworkMessage.getFieldLengthFromBytes(
                        payload, 4 + 4); // offset of 8 for previous, length is one as int is being
        // cast to one byte
        boolean[] masks =
                NetworkMessage.getMaskFromBytes(
                        payload,
                        maskLength,
                        4 + 4); // offset of 8 for previous, this length will be the maskLength
        updateSyncVarsFromBytes(masks, payload, 1 + maskLength + 4 + 4);
    }

    /**
     * Gets id from bytes.
     *
     * @param payload the payload
     * @return the id from bytes
     */
    public static int getComponentIdFromBytes(byte[] payload, int offset) {
        byte[] bytes = Arrays.copyOfRange(payload, offset, offset + 4);

        return NetworkMessage.convertByteArrayToInt(bytes);
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
        for (boolean b : mFieldsMask) {
            if (b) {
                hasTrueInMask = true;
                break;
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
                + "id='"
                + id
                + "', ownerId='"
                + ownerId
                + '\''
                + ", fieldsMask="
                + Arrays.toString(mFieldsMask)
                + ", fields="
                + fieldsString
                + '}';
    }

    /**
     * Gets contents from bytes.
     *
     * @param buff the buff
     * @param offset the offset
     * @return the contents from bytes
     * @throws IOException the io exception
     */
    private void updateSyncVarsFromBytes(boolean[] mask, byte[] buff, int offset)
            throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        final long didSkip = bis.skip(offset);

        ObjectInputStream stream = new ObjectInputStream(bis);

        if (didSkip != offset) return;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                try {
                    Field field = this.mFields[i];
                    ISyncVar obj = (ISyncVar) field.get(this);
                    obj.deserialize(stream);
                } catch (Exception e) {
                    System.out.println("Failed to deserialize " + this.mFields[i].getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public int getOwnerId() {
        return this.ownerId;
    }
}
