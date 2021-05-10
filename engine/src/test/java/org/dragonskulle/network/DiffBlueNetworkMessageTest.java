/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class DiffBlueNetworkMessageTest {
    @Test
    public void testGetMaskFromBytes() throws UnsupportedEncodingException {
        boolean[] actualMaskFromBytes =
                NetworkMessage.getMaskFromBytes("AAAAAAAA".getBytes("UTF-8"));
        assertEquals(8, actualMaskFromBytes.length);
        assertTrue(actualMaskFromBytes[0]);
        assertTrue(actualMaskFromBytes[1]);
        assertTrue(actualMaskFromBytes[2]);
        assertTrue(actualMaskFromBytes[3]);
        assertTrue(actualMaskFromBytes[4]);
        assertTrue(actualMaskFromBytes[5]);
        assertTrue(actualMaskFromBytes[6]);
        assertTrue(actualMaskFromBytes[7]);
    }

    @Test
    public void testGetMaskFromBytes2() {
        boolean[] actualMaskFromBytes =
                NetworkMessage.getMaskFromBytes(new byte[] {0, 'A', 'A', 'A', 'A', 'A', 'A', 'A'});
        assertEquals(8, actualMaskFromBytes.length);
        assertFalse(actualMaskFromBytes[0]);
        assertTrue(actualMaskFromBytes[1]);
        assertTrue(actualMaskFromBytes[2]);
        assertTrue(actualMaskFromBytes[3]);
        assertTrue(actualMaskFromBytes[4]);
        assertTrue(actualMaskFromBytes[5]);
        assertTrue(actualMaskFromBytes[6]);
        assertTrue(actualMaskFromBytes[7]);
    }

    @Test
    public void testConvertBoolArrayToBytes() {
        byte[] actualConvertBoolArrayToBytesResult =
                NetworkMessage.convertBoolArrayToBytes(new boolean[] {true, true, true, true});
        assertEquals(4, actualConvertBoolArrayToBytesResult.length);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[0]);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[1]);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[2]);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[3]);
    }

    @Test
    public void testConvertBoolArrayToBytes2() {
        byte[] actualConvertBoolArrayToBytesResult =
                NetworkMessage.convertBoolArrayToBytes(new boolean[] {false, true, true, true});
        assertEquals(4, actualConvertBoolArrayToBytesResult.length);
        assertEquals((byte) 0, actualConvertBoolArrayToBytesResult[0]);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[1]);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[2]);
        assertEquals((byte) 1, actualConvertBoolArrayToBytesResult[3]);
    }
}