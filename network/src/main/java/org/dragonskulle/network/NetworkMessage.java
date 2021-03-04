package org.dragonskulle.network;

import java.util.*;

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
    //schema
    //::S:: (5bytes)
    //messageType (1Byte)
    //payloadSize (4 bytes)
    //payload (n bytes)
    //::E:: (5 bytes)
    public static void parse(byte[] buff) {
        int i = 0;
        boolean validStart = verifyMessageStart(buff);
        i += 5;
        if (validStart) {
//            System.out.println("Valid Message Start\n");
            byte messageType = getMessageType(buff);
            i += 1;
            int payloadSize = getPayloadSize(buff);
            i += 4;
            byte[] payload = getPayload(buff, messageType, payloadSize);
            i += payloadSize;
            boolean consumedMessage = verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    System.out.println("\nValid Message");
                    System.out.println("Type : " + messageType);
                    System.out.println("Payload : " + Arrays.toString(payload));
                } else {
                    executeClient(messageType, payload);
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

    private static byte[] getPayload(byte[] bytes, byte messageType, int payloadSize) {
        return Arrays.copyOfRange(bytes, 12, 12 + payloadSize);
    }

    public static int convertByteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF) << 0);
    }

    public static byte[] convertIntToByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    private static int getPayloadSize(byte[] bytes) {
        return convertByteArrayToInt(Arrays.copyOfRange(bytes, 6, 11)); //inclusive, exclusive
    }

    private static byte getMessageType(byte[] bytes) {
        return bytes[5];
    }

    public static byte[] build(byte messageType, byte[] payload) {
        assert payload.length <= MAX_TRANSMISSION_SIZE - 15;
        ArrayList<Byte> message = new ArrayList<>(MAX_TRANSMISSION_SIZE);//[MAX_MESSAGE_SIZE];

        //START SIGNATURE
        for (byte b : START_SIGNATURE) {
            message.add(b);
        }

        //MESSAGE TYPE
        message.add(messageType);

        //PAYLOAD SIZE
        byte[] pLength = convertIntToByteArray(payload.length);
        for (byte b : pLength) {
            message.add(b);
        }

        //PAYLOAD
        for (byte b : payload) {
            message.add(b);
        }

        //END SIGNATURE
        for (byte b : END_SIGNATURE) {
            message.add(b);
        }

        return toByteArray(message);
    }

    private static byte[] toByteArray(List<Byte> in) {
        final int n = in.size();
        byte ret[] = new byte[MAX_TRANSMISSION_SIZE];
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
            byte[] payload = getPayload(buff, messageType, payloadSize);
            i += payloadSize;
            boolean consumedMessage = verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    System.out.println("\nValid Message");
                    System.out.println("Type : " + messageType);
                    System.out.println("Payload : " + Arrays.toString(payload));
                } else {
                    executeServer(messageType, payload, sendBytesToClient);
                }
            }
        } else {
            System.out.println("invalid message start");
        }
    }

    private static void executeServer(byte messageType, byte[] payload, Server.SendBytesToClientCurry sendBytesToClient) {
        byte[] message;
        switch (messageType) {
            case (byte) 22:
                System.out.println("Should implement spawn and create building n");
                message = build((byte) 20, "TOSPAWN".getBytes());
                sendBytesToClient.send(message);
                break;
            default:
                System.out.println("Should implement spawn and create building ____");
                message = build((byte) 20, "TOSPAWN".getBytes());
                sendBytesToClient.send(message);
                break;

        }
    }

    private static void executeClient(byte messageType, byte[] payload) {
        switch (messageType) {
            case (byte) 20:
                System.out.println("Should spawn map");
                break;
            case (byte) 21:
                System.out.println("Should spawn capitol");
                break;
            default:
                System.out.println("unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
    }
}
