/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.*;
import lombok.extern.java.Log;

/** @author Oscar L */
@Log
public class NetworkMessage {
    /**
     * Convert byte array of length 4, to int.
     *
     * @param bytes the bytes
     * @return the int
     */
    public static int convertByteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | ((bytes[3] & 0xFF));
    }

    /**
     * Converts int array to a byte array of length 4 * length.
     *
     * @param values the integer values
     * @return the bytes generated
     */
    public static byte[] convertIntsToByteArray(int... values) {
        byte[] bytes = new byte[4 * values.length];

        int cnt = 0;

        for (int value : values) {
            bytes[cnt++] = (byte) (value >> 24);
            bytes[cnt++] = (byte) (value >> 16);
            bytes[cnt++] = (byte) (value >> 8);
            bytes[cnt++] = (byte) value;
        }

        return bytes;
    }

    /**
     * Converts int to a byte array of length 4.
     *
     * @param value the integer
     * @return the bytes generated
     */
    public static byte[] convertIntToByteArray(int value) {
        return convertIntsToByteArray(value);
    }

    /**
     * Gets payload size.
     *
     * @param bytes the bytes
     * @return the payload size
     */
    public static int getPayloadSize(byte[] bytes) {
        return convertByteArrayToInt(Arrays.copyOfRange(bytes, 6, 11)); // inclusive, exclusive
    }

    /**
     * Gets message type as defined in then schema.
     *
     * @param bytes the bytes
     * @return the message type
     */
    public static byte getMessageType(byte[] bytes) {
        return bytes[5];
    }

    /**
     * Converts an Array of Bytes to a byte array.
     *
     * @param in the in
     * @return the byte [ ]
     */
    public static byte[] toByteArray(List<Byte> in) {
        final int n = in.size();
        byte[] ret = new byte[n];
        for (int i = 0; i < n; i++) {
            ret[i] = in.get(i);
        }
        return ret;
    }

    /**
     * Gets field length from bytes.
     *
     * @param buff the buff
     * @param offset the offset
     * @return the field length from bytes
     */
    public static int getFieldLengthFromBytes(byte[] buff, int offset) {
        assert (buff != null);
        return buff[offset];
    }

    /**
     * Gets mask from bytes.
     *
     * @param maskBytes mask in byte form
     * @return the mask from bytes
     */
    public static boolean[] getMaskFromBytes(byte[] maskBytes) {
        boolean[] out = new boolean[maskBytes.length];
        int idx = 0;
        for (byte maskByte : maskBytes) {
            out[idx++] = maskByte != 0;
        }
        return out;
    }

    /**
     * Converts a boolean array into bytes.
     *
     * @param bools the array of booleans
     * @return the bytes from booleans
     */
    public static byte[] convertBoolArrayToBytes(boolean[] bools) {
        ArrayList<Byte> out = new ArrayList<>();
        for (boolean b : bools) out.add(b ? (byte) 1 : (byte) 0);
        /*int bitCounter = 0;
        byte mask = 0;
        for (boolean b : bools) {
            if (b) {
                mask |= 1;
            }
            mask <<= 1;
            bitCounter++;
            if (bitCounter == 8) {
                out.add(mask);
                mask = 0;
                bitCounter = 1;
            }
        }
        if (mask != 0) {
            out.add(mask);
        }*/
        return toByteArray(out);
    }
}
