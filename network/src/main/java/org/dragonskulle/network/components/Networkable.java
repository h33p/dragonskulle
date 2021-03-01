/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.sync.SyncVar;
import sun.misc.IOUtils;

public abstract class Networkable<T> {

    private static final byte[] FIELD_SEPERATOR = {58, 58, 10, 58, 58};
    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */
    private boolean[] fieldsMask;

    private List<Field> fields;

    public void initFields() {
        fields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> SyncVar.class.isAssignableFrom(field.getType()))
                        .collect(Collectors.toList());
    }

    public void connectSyncVars() {
        fields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> SyncVar.class.isAssignableFrom(field.getType()))
                        .collect(Collectors.toList());
        fieldsMask = new boolean[fields.size()];
        System.out.println("created mask");
        int i = 0;
        System.out.println("No. Fields: " + fields.size());
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
        System.out.println("Need to actually link up");
    }

    public byte[] serialize() {
        return constructBufferWithMask();
    }

    public byte[] serializeFully() {
        int maskLength = this.fields.size(); // 1byte
        ArrayList<Byte> mask = new ArrayList<>();
        for (int i = 0; i < maskLength; i++) {
            mask.add((byte) 1);
        }
        ArrayList<Byte> contents = new ArrayList<>();
        //need to add in an id before
        for (int i = 0; i < this.fields.size(); i++) {
            Field f = this.fields.get(i);
            try {
                byte[] syncVarBytes = ((SyncVar) f.get(this)).serialize();
                for (byte b : syncVarBytes) {
                    contents.add(b);
                }
                if (i < this.fields.size() - 1) { //removes trailing seperator
                    for (byte b : FIELD_SEPERATOR) {
                        contents.add(b);
                    }
                }
                this.fieldsMask[i] = false; //reset flag
            } catch (IllegalAccessException | IOException e) {
                e.printStackTrace();
            }

        }
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) maskLength);
        System.out.println("Serialize Fully MASKLENGTH :: " + maskLength);
        payload.addAll(mask);
        System.out.println("Serialize Fully MASK :: " + mask);
        payload.addAll(contents);
        return NetworkMessage.toByteArray(payload);
    }

    public abstract T from(byte[] bytes) throws DecodingException;


    public void updateFromBytes(byte[] payload) throws IOException {
        System.out.println("Creating Networkable from bytes");
        System.out.println("Bytes to update from :: " + Arrays.toString(payload));
        int maskLength = getFieldLengthFromBytes(payload);
        System.out.println("field length :: " + maskLength);
        ArrayList<Boolean> masks = getMaskFromBytes(payload, maskLength);
        System.out.println("Masks :: " + masks);
        ArrayList<SyncVar> contents = getContentsFromBytes(payload, 1 + maskLength);
        System.out.println("SyncVars :: " + contents);
        System.out.println("deserialized component");
        for (int i = 0; i < maskLength; i++) {
            boolean didUpdate = masks.get(i);
            System.out.println("Getting syncvar " + i);
            System.out.println("did update? " + didUpdate);
            if (didUpdate) {
                updateFromMaskOffset(i, contents.get(i));
            }
        }
    }

    private byte[] constructBufferWithMask() {
        int maskLength = this.fields.size(); // 1byte
        ArrayList<Byte> mask = new ArrayList<>(maskLength);
        ArrayList<Byte> contents = new ArrayList<>();
        //need to add in an id before
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
                    this.fieldsMask[i] = false; //reset flag
                } catch (IllegalAccessException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                mask.add((byte) 0);
            }
        }
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) maskLength);
        payload.addAll(mask);
        payload.addAll(contents);
        return NetworkMessage.toByteArray(payload);
    }

    private ArrayList<SyncVar> getContentsFromBytes(
            byte[] buff, int offset) throws IOException {
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
                        System.out.println("trying to deserialise to syncvar :: " + syncVarBytes);
                        out.add(SyncVar.deserialize(NetworkMessage.toByteArray(syncVarBytes)));
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
                    System.out.println("trying to deserialise to syncvar :: " + syncVarBytes);
                    out.add(SyncVar.deserialize(NetworkMessage.toByteArray(syncVarBytes)));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return out;
    }

    private ArrayList<Boolean> getMaskFromBytes(byte[] buff, int maskLength) {
        ArrayList<Boolean> out = new ArrayList<>();
        byte[] maskBytes = Arrays.copyOfRange(buff, 1, 1 + maskLength);
        for (byte maskByte : maskBytes) {
            if (maskByte == (byte) 1) {
                out.add(true);
            } else {
                System.out.println("Decrypting bool byte :: " + maskByte);
                out.add(false);
            }
        }
        return out;
    }

    private int getFieldLengthFromBytes(byte[] buff) {
        assert (buff != null);
        return buff[0];
    }

    private void updateFromMaskOffset(int offset, SyncVar newValue) {
        try {
            System.out.println("[updateFromMaskOffset] getting " + offset);
            System.out.println("[updateFromMaskOffset] fields " + this.fields);

            Field E = this.fields.get(offset);
            System.out.println("[updateFromMaskOffset] setting");
            E.set(this, newValue);
            System.out.println("[updateFromMaskOffset] done");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void handleFieldChange(int maskId) {
        this.fieldsMask[maskId] = true;
    }

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
}
