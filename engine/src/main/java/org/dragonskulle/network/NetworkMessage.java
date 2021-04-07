/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.*;

/** @author Oscar L */
public class NetworkMessage {
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
