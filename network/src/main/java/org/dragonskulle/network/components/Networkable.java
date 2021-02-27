/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.sync.SyncVar;
import sun.misc.IOUtils;

public class Networkable {

    private static final byte[] FIELD_SEPERATOR = {58, 58, 10, 58, 58};
    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */
    private boolean[] fieldsMask;

    private List<Field> fields;

    //    final NetworkClient client;
    public Networkable() {
        //        this.client = object;
        System.out.println("client is assigned");
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

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        oos = new ObjectOutputStream(bos);
        byte[] buff = constructBufferWithMask();
        oos.write(buff);
        oos.flush();
        System.out.println("serialized component " + bos.size());
        return bos.toByteArray();
    }

    public void updateFromBytes(byte[] buff) throws IOException {
        int maskLength = getFieldLengthFromBytes(buff);
        ArrayList<Boolean> masks = getMaskFromBytes(buff, maskLength);
        ArrayList<SyncVar> contents = getContentsFromBytes(buff, 1 + maskLength, masks);
        System.out.println("deserialized component");
        for (int i = 0; i < Objects.requireNonNull(masks).size(); i++) {
            boolean didUpdate = masks.get(i);
            if (didUpdate) {
                updateFromMaskOffset(i, contents.get(i));
            }
        }
    }

    private byte[] constructBufferWithMask() {
        int maskLength = this.fields.size(); // 1byte
        ArrayList<Byte> mask = new ArrayList<>(maskLength);
        ArrayList<Byte> contents = new ArrayList<>();
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
                    for (byte b : FIELD_SEPERATOR) {
                        contents.add(b);
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
            byte[] buff, int offset, ArrayList<Boolean> masks) throws IOException {
        // TODO
        // ignore n offset bytes, then read until SEPERATOR then deserialize syncvar;
        ArrayList<SyncVar> out = new ArrayList<>();
        ArrayList<Byte> syncVarBytes;
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        final long didSkip = bis.skip(offset); // ignores the mask length and mask bytes
        if (didSkip == offset) {
            syncVarBytes = new ArrayList<>();
            while (bis.available() > 0) {
                bis.mark(FIELD_SEPERATOR.length);
                byte[] nextFiveBytes = IOUtils.readExactlyNBytes(bis, FIELD_SEPERATOR.length);
                bis.reset();
                if (Arrays.equals(nextFiveBytes, FIELD_SEPERATOR)) {
                    // seek field bytes
                    IOUtils.readExactlyNBytes(bis, 5);
                    // end of sync var;
                    // try to deserialize.
                    try {
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
        }
        return out;
    }

    private ArrayList<Boolean> getMaskFromBytes(byte[] buff, int maskLength) {
        ArrayList<Boolean> out = new ArrayList<>();
        byte[] maskBytes = Arrays.copyOfRange(buff, 1, 1 + maskLength);
        for (byte maskByte : maskBytes) {
            if ((int) maskByte == 1) {
                out.add(true);
            } else {
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
            this.fields.get(offset).set(this, newValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void handleFieldChange(int maskId) {
        this.fieldsMask[maskId] = true;
        System.out.println("set mask " + maskId);
    }

    public boolean hasBeenModified() {
        boolean hasTrueInMask = false;
        for (boolean b : fieldsMask) {
            if (b) {
                hasTrueInMask = true;
                break;
            }
        }
        System.out.println("has networkable instance been modified? :: " + hasTrueInMask);
        return hasTrueInMask;
    }
}
