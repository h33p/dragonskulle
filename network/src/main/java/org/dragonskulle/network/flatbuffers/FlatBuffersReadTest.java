/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.flatbuffers;

import java.io.*;
import org.dragonskulle.network.proto.BaseMessage;
import org.dragonskulle.network.proto.CreateCityRequest;

public class FlatBuffersReadTest {
    public static void main(String[] args) {
        try {
            System.out.println("reading Bytes");
            RandomAccessFile f = new RandomAccessFile("CreateCityMessage.bin", "r");
            byte[] buf = new byte[(int) f.length()];
            f.readFully(buf);
            System.out.println("read Bytes");
            System.out.println("wrapping Bytes");
            java.nio.ByteBuffer wrappedBuf = java.nio.ByteBuffer.wrap(buf);
            System.out.println("extracting base message");

            BaseMessage message = BaseMessage.getRootAsBaseMessage(wrappedBuf);
            System.out.println("Message code: " + message.code());
            if (message.code() == 1) {
                // switch depending on code
                CreateCityRequest request =
                        (CreateCityRequest) message.data(new CreateCityRequest());
                assert request != null;
                System.out.println("Read request to build city from " + request.owner());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
