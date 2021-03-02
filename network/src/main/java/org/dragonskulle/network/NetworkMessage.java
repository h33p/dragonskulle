/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import sun.misc.IOUtils;

public class NetworkMessage {
    private static final int MAX_TRANSMISSION_SIZE = NetworkConfig.MAX_TRANSMISSION_SIZE;
    private static final byte[] START_SIGNATURE = {58, 58, 83, 58, 58};
    private static final byte[] END_SIGNATURE = {58, 58, 69, 58, 58};

    /*payload byte codes
    0 : Print Contents [DEBUG]
    20-50 : spawn
    20 : spawn map
    21 : spawn capitol
    * */
    // schema
    // ::S:: (5bytes)
    // messageType (1Byte)
    // payloadSize (4 bytes)
    // payload (n bytes)
    // ::E:: (5 bytes)

    public static void parse(byte[] buff, NetworkClient client) {
        int i = 0;
        boolean validStart = verifyMessageStart(buff);
        i += 5;
        if (validStart) {
            //            System.out.println("Valid Message Start\n");
            byte messageType = getMessageType(buff);
            i += 1;
            int payloadSize = getPayloadSize(buff);
            i += 4;
            byte[] payload = getPayload(buff, messageType, i, payloadSize);
            i += payloadSize;
            boolean consumedMessage = verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) { // debug type
                    System.out.println("\nValid Message");
                    System.out.println("Type : " + messageType);
                    System.out.println("Payload : " + Arrays.toString(payload));
                } else {
                    client.executeBytes(messageType, payload);
                }
            }
        } else {
            System.out.println("invalid message start");
        }
    }

    private static boolean verifyMessageEnd(int offset, byte[] bytes) {
        try {
            byte[] consumedSignature = Arrays.copyOfRange(bytes, offset, offset + 5);
            return (Arrays.equals(END_SIGNATURE, consumedSignature));
        } catch (NullPointerException e) {
            return false;
        }
    }

    private static boolean verifyMessageStart(byte[] bytes) {
        byte[] consumedSignature = Arrays.copyOfRange(bytes, 0, 5);
        return (Arrays.equals(START_SIGNATURE, consumedSignature));
    }

    private static byte[] getPayload(byte[] bytes, byte messageType, int offset, int payloadSize) {
        byte[] payload = Arrays.copyOfRange(bytes, offset, offset + payloadSize);
        return payload;
    }

    public static int convertByteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | ((bytes[3] & 0xFF) << 0);
    }

    public static byte[] convertIntToByteArray(int value) {
        return new byte[] {
            (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
        };
    }

    private static int getPayloadSize(byte[] bytes) {
        return convertByteArrayToInt(Arrays.copyOfRange(bytes, 6, 11)); // inclusive, exclusive
    }

    private static byte getMessageType(byte[] bytes) {
        return bytes[5];
    }

    public static byte[] build(byte messageType, byte[] payload) {
        assert payload.length <= MAX_TRANSMISSION_SIZE - 15;
        ArrayList<Byte> message = new ArrayList<>(MAX_TRANSMISSION_SIZE); // [MAX_MESSAGE_SIZE];

        // START SIGNATURE
        for (byte b : START_SIGNATURE) {
            message.add(b);
        }

        // MESSAGE TYPE
        message.add(messageType);

        // PAYLOAD SIZE
        byte[] pLength = convertIntToByteArray(payload.length);
        for (byte b : pLength) {
            message.add(b);
        }

        // PAYLOAD
        for (byte b : payload) {
            message.add(b);
        }

        message.addAll(
                new ArrayList<>(
                        MAX_TRANSMISSION_SIZE
                                - 15
                                - payload.length)); // adding null bytes to fill message size
        // END SIGNATURE
        for (byte b : END_SIGNATURE) {
            message.add(b);
        }

        return toByteArray(message);
    }

    public static byte[] toByteArray(List<Byte> in) {
        final int n = in.size();
        byte ret[] = new byte[n];
        for (int i = 0; i < n; i++) {
            ret[i] = in.get(i);
        }
        return ret;
    }

    public static void parse(byte[] buff, Server.SendBytesToClientCurry sendBytesToClient) {
        int i = 0;
        boolean validStart = verifyMessageStart(buff);
        i += 5;
        if (validStart) {
            //            System.out.println("Valid Message Start\n");
            byte messageType = getMessageType(buff);
            i += 1;
            int payloadSize = getPayloadSize(buff);
            i += 4;
            byte[] payload = getPayload(buff, messageType, i, payloadSize);
            i += payloadSize;
            boolean consumedMessage = verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    System.out.println("\nValid Message");
                    System.out.println("Type : " + messageType);
                    System.out.println("Payload : " + Arrays.toString(payload));
                } else {
                    Server.executeBytes(messageType, payload, sendBytesToClient);
                }
            }
        } else {
            System.out.println("invalid message start");
        }
    }

    public static byte[] readMessageFromStream(BufferedInputStream bIn) throws IOException {
        byte[] byteHeader = IOUtils.readExactlyNBytes(bIn, 10);
        boolean validStart = verifyMessageStart(byteHeader);
        byte[] bArray;
        int toRead = 0;
        if (validStart) {
            toRead = getPayloadSize(byteHeader) + 5; // read to end of payload and trailer
        }
        bArray = IOUtils.readExactlyNBytes(bIn, toRead);

        return concatenate(byteHeader, bArray);
    }

    private static <T> T concatenate(T a, T b) {
        if (!a.getClass().isArray() || !b.getClass().isArray()) {
            throw new IllegalArgumentException();
        }

        Class<?> resCompType;
        Class<?> aCompType = a.getClass().getComponentType();
        Class<?> bCompType = b.getClass().getComponentType();

        if (aCompType.isAssignableFrom(bCompType)) {
            resCompType = aCompType;
        } else if (bCompType.isAssignableFrom(aCompType)) {
            resCompType = bCompType;
        } else {
            throw new IllegalArgumentException();
        }

        int aLen = Array.getLength(a);
        int bLen = Array.getLength(b);

        @SuppressWarnings("unchecked")
        T result = (T) Array.newInstance(resCompType, aLen + bLen);
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);

        return result;
    }
}
