/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkableComponent;
import sun.misc.IOUtils;

/**
 * @author Oscar L
 *     <p>The Network message structure which is sent. It will follow the format below. 0 : Print
 *     Contents [DEBUG] 20-50 : spawn 20 : spawn map 21 : spawn capitol
 *     <p>// schema // ::S:: (5bytes) // messageType (1Byte) // payloadSize (4 bytes) // payload (n
 *     bytes) // ::E:: (5 bytes)
 */
public class NetworkMessage {
    /** The constant MAX_TRANSMISSION_SIZE. */
    private static final int MAX_TRANSMISSION_SIZE = NetworkConfig.MAX_TRANSMISSION_SIZE;
    /** The constant START_SIGNATURE. */
    private static final byte[] START_SIGNATURE = {58, 58, 83, 58, 58};
    /** The constant END_SIGNATURE. */
    private static final byte[] END_SIGNATURE = {58, 58, 69, 58, 58};
    /** The constant FIELD_SEPERATOR. */
    public static final byte[] FIELD_SEPERATOR = {58, 58, 10, 58, 58};

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

    /**
     * Parse bytes.
     *
     * @param buff the buff
     * @param client the client
     */
    public static byte parse(byte[] buff, NetworkClient client) {
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
                    return client.executeBytes(messageType, payload);
                }
            }
        } else {
            System.out.println("invalid message start");
        }
        return (byte) -1;
    }

    /**
     * Verify the message ends correctly.
     *
     * @param offset the offset of the original message to the end trailer
     * @param bytes the bytes
     * @return the boolean
     */
    public static boolean verifyMessageEnd(int offset, byte[] bytes) {
        try {
            byte[] consumedSignature = Arrays.copyOfRange(bytes, offset, offset + 5);
            return (Arrays.equals(END_SIGNATURE, consumedSignature));
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Verify message starts correctly.
     *
     * @param bytes the bytes
     * @return the boolean
     */
    public static boolean verifyMessageStart(byte[] bytes) {
        byte[] consumedSignature = Arrays.copyOfRange(bytes, 0, 5);
        return (Arrays.equals(START_SIGNATURE, consumedSignature));
    }

    /**
     * Get payload from the whole message.
     *
     * @param bytes the bytes
     * @param messageType the message type
     * @param offset the offset of the original message to the payload
     * @param payloadSize the payload size
     * @return the byte [ ]
     */
    public static byte[] getPayload(byte[] bytes, byte messageType, int offset, int payloadSize) {
        byte[] payload = Arrays.copyOfRange(bytes, offset, offset + payloadSize);
        return payload;
    }

    /**
     * Convert byte array to int int.
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
     * Convert int to byte array byte [ ].
     *
     * @param value the value
     * @return the byte [ ]
     */
    public static byte[] convertIntToByteArray(int value) {
        return new byte[] {
            (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
        };
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
     * Builds a message to be transmitted over the socket connection.
     *
     * @param messageType the message type
     * @param payload the payload
     * @return the bytes to be sent
     */
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
     * Read message from the input byte stream.
     *
     * @param bIn the b in
     * @return the byte [ ]
     * @throws IOException the io exception
     */
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

    /**
     * Concatenates two arrays.
     *
     * @param <T> the type parameter
     * @param a the a
     * @param b the b
     * @return the t
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
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
     * @param buff the buff
     * @param maskLength the mask length
     * @param offset the offset
     * @return the mask from bytes
     */
    public static boolean[] getMaskFromBytes(byte[] buff, int maskLength, int offset) {
        byte[] maskBytes = Arrays.copyOfRange(buff, 1 + offset, 1 + maskLength + offset);
        boolean[] out = new boolean[maskBytes.length];
        int idx = 0;
        for (byte maskByte : maskBytes) {
            out[idx++] = maskByte != 0;
        }
        return out;
    }

    public static byte[] convertBoolArrayToBytes(boolean[] didChildUpdateMask) {
        ArrayList<Byte> out = new ArrayList<>();
        int bitCounter = 0;
        byte mask = 0;
        for (boolean b : didChildUpdateMask) {
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
        }
        return toByteArray(out);
    }

    public static byte getChildClassTypeByte(Class<? extends NetworkableComponent> aClass) {
        if (aClass == Capital.class) {
            return (byte) 21;
        }
        return (byte) 0;
    }

    public static Class<? extends NetworkableComponent> getChildClassFromByte(byte bClass) {
        if (bClass == (byte) 21) {
            return Capital.class;
        }

        return null;
    }
}
