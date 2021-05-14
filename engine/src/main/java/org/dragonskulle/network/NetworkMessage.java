/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Collection;
import java.util.List;

/**
 * Contains utilities for network message processing.
 *
 * @author Oscar L
 * @author Aurimas Blažulionis
 */
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
        return (boolCount + 7) / 8;
    }

    /**
     * Gets mask from bytes.
     *
     * @param maskBytes mask in byte form
     * @return the mask from bytes
     */
    public static boolean[] getMaskFromBytes(byte[] maskBytes) {
        boolean[] out = new boolean[maskBytes.length * 8];
        int idx = 0;
        for (byte maskByte : maskBytes) {
            for (int i = 0; i < 8; i++) {
                out[idx++] = (maskByte & (1 << i)) != 0;
            }
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
        byte[] out = new byte[maskSizeInBytes(bools.length)];

        int idx = 0;

        for (boolean flag : bools) {
            if (flag) {
                out[idx / 8] |= 1 << (idx % 8);
            }
            idx++;
        }

        return out;
    }

    /**
     * Converts a collection mask into bytes.
     *
     * @param bools the collection of booleans
     * @return the bytes from booleans
     */
    public static byte[] convertCollectionMaskToBytes(Collection<Boolean> bools) {
        byte[] out = new byte[maskSizeInBytes(bools.size())];

        int idx = 0;

        for (boolean flag : bools) {
            if (flag) {
                out[idx / 8] |= 1 << (idx % 8);
            }
            idx++;
        }

        return out;
    }
}
