/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import com.sun.istack.internal.NotNull;
import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.sync.SyncVar;
import sun.misc.IOUtils;

/**
 * Any component that extends this, its syncvars will be updated with the server.
 *
 * @param <T> the type parameter
 */
public abstract class Networkable<T> {

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * The Id.
     */
    private String id = UUID.randomUUID().toString();

    /**
     * The constant FIELD_SEPERATOR. This is between all fields in the serialization.
     */
    private static final byte[] FIELD_SEPERATOR = {58, 58, 10, 58, 58};

    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */
    private boolean[] fieldsMask;

    /**
     * The Fields.
     */
    private List<Field> fields;

    /**
     * Init fields.
     */
    public void initFields() {
        fields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> SyncVar.class.isAssignableFrom(field.getType()))
                        .collect(Collectors.toList());
    }

    /**
     * Connect sync vars.
     */
    public void connectSyncVars() {
        fields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> SyncVar.class.isAssignableFrom(field.getType()))
                        .collect(Collectors.toList());
        fieldsMask = new boolean[fields.size()];
        int i = 0;
        for (Field f : fields) {
            try {
                SyncVar sv =
                        (SyncVar) f.get(this); // retrieves syncvar from child and adds it to the
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
        int maskLength = this.fields.size(); // 1byte
        ArrayList<Byte> mask = new ArrayList<>(maskLength);
        ArrayList<Byte> contents = new ArrayList<>();
        // need to add in an id before
        for (int i = 0; i < this.fields.size(); i++) {
            boolean didVarChange = this.fieldsMask[i];
            Field f = this.fields.get(i);
            if (didVarChange) {
                try {
                    mask.add((byte) 1);
                    byte[] syncVarBytes = ((SyncVar) f.get(this)).serialize();
                    for (byte b : syncVarBytes) {
                        contents.add(b);
                    }
                    if (i < this.fields.size() - 1) {
                        for (byte b : FIELD_SEPERATOR) {
                            contents.add(b);
                        }
                    }
                    this.fieldsMask[i] = false; // reset flag
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
        ArrayList<Byte> networkId = new ArrayList<Byte>(); // 36 bytes
        for (Byte aByte : this.getId().getBytes()) {
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
        int maskLength = this.fields.size(); // 1byte
        ArrayList<Byte> mask = new ArrayList<>();
        for (int i = 0; i < maskLength; i++) {
            mask.add((byte) 1);
        }
        ArrayList<Byte> contents = new ArrayList<>();
        for (int i = 0; i < this.fields.size(); i++) {
            Field f = this.fields.get(i);
            try {
                byte[] syncVarBytes = ((SyncVar) f.get(this)).serialize();
                for (byte b : syncVarBytes) {
                    contents.add(b);
                }
                if (i < this.fields.size() - 1) { // removes trailing seperator
                    for (byte b : FIELD_SEPERATOR) {
                        contents.add(b);
                    }
                }
                this.fieldsMask[i] = false; // reset flag
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
     * @param <T>    the type parameter
     * @param target the target
     * @param bytes  the bytes
     * @return the component
     * @throws DecodingException thrown if error in decoding
     */
    public static <T extends Networkable> T from(Class<T> target, byte[] bytes)
            throws DecodingException {
        try {
            T t = target.newInstance();
            t.initFields();
            t.updateFromBytes(bytes);
            return t;
        } catch (IOException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new DecodingException("Error decoding capitol from bytes");
        }
    }

    /**
     * Update fields from bytes.
     *
     * @param payload the payload
     * @throws IOException the io exception
     */
    public void updateFromBytes(byte[] payload) throws IOException {
        String id = getIdFromBytes(payload);
        this.setId(id);
        int maskLength = getFieldLengthFromBytes(payload, 36); // offset of 36 to ignore netid
        ArrayList<Boolean> masks =
                getMaskFromBytes(payload, maskLength, 36); // offset of 36 to ignore netid
        ArrayList<SyncVar> contents =
                getContentsFromBytes(payload, 1 + maskLength + 36); // offset of 36 to ignore netid
        for (int i = 0; i < maskLength; i++) {
            boolean didUpdate = masks.get(i);
            if (didUpdate) {
                updateFromMaskOffset(i, contents.get(i));
            }
        }
    }

    /**
     * Gets id from bytes.
     *
     * @param payload the payload
     * @return the id from bytes
     */
    public static String getIdFromBytes(byte[] payload) {
        return new String(Arrays.copyOf(payload, 36), Charset.defaultCharset());
    }

    /**
     * Gets contents from bytes.
     *
     * @param buff   the buff
     * @param offset the offset
     * @return the contents from bytes
     * @throws IOException the io exception
     */
    private static ArrayList<SyncVar> getContentsFromBytes(byte[] buff, int offset)
            throws IOException {
        ArrayList<SyncVar> out = new ArrayList<>();
        ArrayList<Byte> syncVarBytes;
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        final long didSkip = bis.skip(offset); // ignores the mask length and mask bytes
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
                        out.add(SyncVar.deserialize(NetworkMessage.toByteArray(syncVarBytes)));
                        syncVarBytes.clear(); // clears current sync bytes that have been read
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (byte b : IOUtils.readNBytes(bis, 1)) {
                        syncVarBytes.add(b); // read one byte from stream
                    }
                }
            }
            if (!syncVarBytes.isEmpty()) {
                try {
                    out.add(SyncVar.deserialize(NetworkMessage.toByteArray(syncVarBytes)));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return out;
    }

    /**
     * Gets mask from bytes.
     *
     * @param buff       the buff
     * @param maskLength the mask length
     * @param offset     the offset
     * @return the mask from bytes
     */
    private static ArrayList<Boolean> getMaskFromBytes(byte[] buff, int maskLength, int offset) {
        ArrayList<Boolean> out = new ArrayList<>();
        byte[] maskBytes = Arrays.copyOfRange(buff, 1 + offset, 1 + maskLength + offset);
        for (byte maskByte : maskBytes) {
            if (maskByte == (byte) 1) {
                out.add(true);
            } else {
                out.add(false);
            }
        }
        return out;
    }

    /**
     * Gets field length from bytes.
     *
     * @param buff   the buff
     * @param offset the offset
     * @return the field length from bytes
     */
    private static int getFieldLengthFromBytes(byte[] buff, int offset) {
        assert (buff != null);
        return buff[offset];
    }

    /**
     * Updates one field from mask offset.
     *
     * @param offset   the offset
     * @param newValue the new value
     */
    private void updateFromMaskOffset(int offset, SyncVar newValue) {
        try {
            //            System.out.println("[updateFromMaskOffset] getting " + offset);
            //            System.out.println("[updateFromMaskOffset] fields " +
            // this.fields.get(offset));
            Field E = this.fields.get(offset);
            //            System.out.println("[updateFromMaskOffset] setting field from " +
            // newValue.getClass());
            E.set(this, newValue);
            //            System.out.println("[updateFromMaskOffset] done");
        } catch (IllegalAccessException | IllegalArgumentException e) {
            System.out.println("error setting field in instance");
            e.printStackTrace();
        }
    }

    /**
     * Handle field change, sets mask to true when field has been edited.
     *
     * @param maskId the mask id
     */
    private void handleFieldChange(int maskId) {
        this.fieldsMask[maskId] = true;
    }

    /**
     * Has been field been modified?.
     *
     * @return the boolean
     */
    public boolean hasBeenModified() {
        boolean hasTrueInMask = false;
        for (boolean b : fieldsMask) {
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
        for (Field field : fields) {
            try {
                fieldsString.append(field.get(this).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        fieldsString.append("\n}");

        return "Networkable{"
                + "id='"
                + id
                + '\''
                + ", fieldsMask="
                + Arrays.toString(fieldsMask)
                + ", fields="
                + fieldsString
                + '}';
    }
}