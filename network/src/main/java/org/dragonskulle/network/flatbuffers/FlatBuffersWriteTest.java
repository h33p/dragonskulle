/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.flatbuffers;

import com.google.flatbuffers.FlatBufferBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.dragonskulle.network.proto.*;

public class FlatBuffersWriteTest {
    public static void main(String[] args) {
        System.out.println("Creating Builder");

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int messageCode = 1;
        System.out.println("Creating CreateCityRequest");

        int createCityRequest = CreateCityRequest.createCreateCityRequest(builder, 100);

        System.out.println("Creating BaseMessage");
        BaseMessage.startBaseMessage(builder);
        BaseMessage.addCode(builder, 1);
        BaseMessage.addDataType(builder, Any.CreateCityRequest);
        BaseMessage.addData(builder, createCityRequest);
        int builtMessage = BaseMessage.endBaseMessage(builder);
        System.out.println("Finishing Builder");
        builder.finish(builtMessage);
        System.out.println("Getting bytes from Builder");

        byte[] buf = builder.sizedByteArray();

        FileOutputStream out = null;
        try {
            File outFile = new File("CreateCityMessage.bin");
            out = new FileOutputStream(outFile);
            out.write(buf);
            out.close();
            System.out.println("wrote flatbuffer bytes to : " + outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
