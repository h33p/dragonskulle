/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * Calculates how many bytes input booleans use.
     *
     * @param boolCount number of booleans that are to be packed together
     * @return how many bytes this mask would take
     */
    public static int maskSizeInBytes(int boolCount) {
        return boolCount;
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
        for (boolean b : bools) {
            out.add(b ? (byte) 1 : (byte) 0);
        }
        return toByteArray(out);
    }

    /**
     * Converts a collection mask into bytes.
     *
     * @param bools the collection of booleans
     * @return the bytes from booleans
     */
    public static byte[] convertCollectionMaskToBytes(Collection<Boolean> bools) {
        ArrayList<Byte> out = new ArrayList<>();
        for (boolean b : bools) {
            out.add(b ? (byte) 1 : (byte) 0);
        }
        return toByteArray(out);
    }
}
