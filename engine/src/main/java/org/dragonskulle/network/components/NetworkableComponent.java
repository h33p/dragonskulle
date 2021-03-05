/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import org.dragonskulle.components.Component;
import org.dragonskulle.network.DecodingException;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.sync.ISyncVar;
import org.jetbrains.annotations.NotNull;
import sun.misc.IOUtils;

/**
 * @author Oscar L Any component that extends this, its syncvars will be updated with the server.
 * @param <T> the type parameter
 */
public abstract class NetworkableComponent<T> extends Component {

    NetworkableComponent(int networkComponentId) {
        this.id = networkComponentId;
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

    /** The Id. */
    private int id;

    /** The constant FIELD_SEPERATOR. This is between all fields in the serialization. */
    private static final byte[] FIELD_SEPERATOR = {58, 58, 10, 58, 58};

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
     * @return the bytes of the component
     */
    public byte[] serialize() {
        ArrayList<Byte> networkId = getIdBytes();
        int maskLength = this.mFields.length; // 1byte
        ArrayList<Byte> mask = new ArrayList<>(maskLength);
        ArrayList<Byte> contents = new ArrayList<>();
        // need to add in an id before
        for (int i = 0; i < this.mFields.length; i++) {
            boolean didVarChange = this.mFieldsMask[i];
            Field f = this.mFields[i];
            if (didVarChange) {
                try {
                    mask.add((byte) 1);
                    byte[] syncVarBytes = ((ISyncVar) f.get(this)).serialize();
                    for (byte b : syncVarBytes) {
                        contents.add(b);
                    }
                    if (i < this.mFields.length - 1) {
                        for (byte b : FIELD_SEPERATOR) {
                            contents.add(b);
                        }
                    }
                    this.mFieldsMask[i] = false; // reset flag
                } catch (IllegalAccessException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                mask.add((byte) 0);
            }
        }
        ArrayList<Byte> payload = new ArrayList<>();
        payload.addAll(networkId);
        payload.add((byte) maskLength);
        payload.addAll(mask);
        payload.addAll(contents);
        return NetworkMessage.toByteArray(payload);
    }

    /**
     * Gets the bytes of the id.
     *
     * @return the bytes
     */
    @NotNull
    private ArrayList<Byte> getIdBytes() {
        ArrayList<Byte> networkId = new ArrayList<>(); // 4 bytes
        byte[] bytes = NetworkMessage.convertIntToByteArray(this.getId());
        for (Byte aByte : bytes) {
            networkId.add(aByte);
        }
        return networkId;
    }

    /**
     * Serialize fully byte, is this ran on spawn when the whole component needs creating.
     *
     * @return the bytes
     */
    public byte[] serializeFully() {
        ArrayList<Byte> networkId = getIdBytes();
        int maskLength = this.mFields.length; // 1byte
        ArrayList<Byte> mask = new ArrayList<>();
        for (int i = 0; i < maskLength; i++) {
            mask.add((byte) 1);
        }
        ArrayList<Byte> contents = new ArrayList<>();
        for (int i = 0; i < this.mFields.length; i++) {
            Field f = this.mFields[i];
            try {
                byte[] syncVarBytes = ((ISyncVar) f.get(this)).serialize();
                for (byte b : syncVarBytes) {
                    contents.add(b);
                }
                if (i < this.mFields.length - 1) { // removes trailing seperator
                    for (byte b : FIELD_SEPERATOR) {
                        contents.add(b);
                    }
                }
                this.mFieldsMask[i] = false; // reset flag
            } catch (IllegalAccessException | IOException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Byte> payload = new ArrayList<>();
        payload.addAll(networkId);
        payload.add((byte) maskLength);
        payload.addAll(mask);
        payload.addAll(contents);
        return NetworkMessage.toByteArray(payload);
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
        int id = getIdFromBytes(payload);
        this.setId(id);
        int maskLength =
                NetworkMessage.getFieldLengthFromBytes(payload, 4); // offset of 4 to ignore netid
        boolean[] masks =
                NetworkMessage.getMaskFromBytes(
                        payload, maskLength, 4); // offset of 4 to ignore netid
        updateSyncVarsFromBytes(masks, payload, 1 + maskLength + 4); // offset of 4 to ignore netid
    }

    /**
     * Gets id from bytes.
     *
     * @param payload the payload
     * @return the id from bytes
     */
    public static int getIdFromBytes(byte[] payload) {
        return NetworkMessage.convertByteArrayToInt(Arrays.copyOf(payload, 4));
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
    private ArrayList<ISyncVar> updateSyncVarsFromBytes(boolean[] mask, byte[] buff, int offset)
            throws IOException {
        ArrayList<ISyncVar> out = new ArrayList<>();
        ArrayList<Byte> syncVarBytes;
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        final long didSkip = bis.skip(offset); // ignores the mask length and mask bytes

        int fieldIndex = -1;

        if (didSkip == offset) {
            syncVarBytes = new ArrayList<>();
            while (bis.available() > 0) {
                bis.mark(FIELD_SEPERATOR.length);
                byte[] nextFiveBytes = IOUtils.readNBytes(bis, FIELD_SEPERATOR.length);
                bis.reset();
                if (Arrays.equals(nextFiveBytes, FIELD_SEPERATOR)) {
                    // seek field bytes
                    IOUtils.readExactlyNBytes(bis, 5);
                    // end of sync var;
                    // try to deserialize.
                    try {
                        // Go to the field we need
                        while (!mask[++fieldIndex]) ;

                        System.out.println("[updateSyncVarsFromBytes] getting " + fieldIndex);
                        Field field = this.mFields[fieldIndex];
                        System.out.println("[updateSyncVarsFromBytes] field " + field);
                        ISyncVar obj = (ISyncVar) field.get(this);
                        System.out.println("[updateSyncVarsFromBytes] deserializing");

                        obj.deserialize(NetworkMessage.toByteArray(syncVarBytes));
                        System.out.println("[updateSyncVarsFromBytes] done");
                        syncVarBytes.clear(); // clears current sync bytes that have been read
                    } catch (ClassNotFoundException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (byte b : IOUtils.readNBytes(bis, 1)) {
                        syncVarBytes.add(b); // read one byte from stream
                    }
                }
            }
            if (!syncVarBytes.isEmpty()) {
                // Go to the field we need
                while (!mask[++fieldIndex]) ;
                try {
                    System.out.println("[updateSyncVarsFromBytes] getting " + fieldIndex);
                    Field field = this.mFields[fieldIndex];
                    System.out.println("[updateSyncVarsFromBytes] field " + field);
                    ISyncVar obj = (ISyncVar) field.get(this);
                    System.out.println("[updateSyncVarsFromBytes] deserializing");

                    obj.deserialize(NetworkMessage.toByteArray(syncVarBytes));
                    System.out.println("[updateSyncVarsFromBytes] done");
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return out;
    }
}
