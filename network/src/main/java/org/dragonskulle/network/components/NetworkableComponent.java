package org.dragonskulle.network.components;


import org.dragonskulle.components.Component;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.sync.SyncVar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkableComponent extends Component implements Serializable {
    private static final byte[] FIELD_SEPERATOR = {58, 58, 10, 58, 58};
    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */

    private boolean[] fieldsMask;
    private List<Field> fields;

    //    final NetworkClient client;
    public NetworkableComponent() {
//        this.client = object;
        System.out.println("client is assigned");
        this.connectSyncVars();
    }

    void dispose() {
//        this.client.dispose();
    }

    void connectSyncVars() {
        fields = Arrays.stream(this.getClass().getDeclaredFields()).filter(field -> SyncVar.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
        fieldsMask = new boolean[fields.size()];
        System.out.println("created mask");
        int i = 0;
        System.out.println("No. Fields: " + fields.size());
        for (Field f : fields) {
            try {
                SyncVar sv = (SyncVar) f.get(this); //retrieves syncvar from child and adds it to the connection array
                System.out.println("adding " + f.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Need to actually link up"); //TODO component should create a network object which this can talk to so i can store my vars there
        //TODO create a wrapper around component with NetworkComponent which has storage for vars.
    }

    public byte[] serialize() throws IOException {
        //TODO be replaced with custom serialize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        oos = new ObjectOutputStream(bos);
        byte[] buff = constructBufferWithMask();
        oos.write(buff);
        oos.flush();
        System.out.println("serialized componment " + bos.size());
        return bos.toByteArray();
    }

    private byte[] constructBufferWithMask() {
        ArrayList<Byte> mask = new ArrayList<>(this.fieldsMask.length);
        ArrayList<Byte> contents = new ArrayList<>();
        for (int i = 0; i < this.fields.size(); i++) {
            boolean didVarChange = this.fieldsMask[i];
            Field f = this.fields.get(i);
            if (didVarChange) {
                try {
                    mask.add((byte) 1);
                    byte[] syncVarBytes = ((SyncVar) f.get(this)).serialize();
                    for (byte b : syncVarBytes) {
                        contents.add(b);
                    }
                    for (byte b : FIELD_SEPERATOR) {
                        contents.add(b);
                    }
                } catch (IllegalAccessException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                mask.add((byte) 0);
            }
        }
        ArrayList<Byte> payload = new ArrayList<>();
        payload.addAll(mask);
        payload.addAll(contents);
        return NetworkMessage.toByteArray(payload);
    }


//
//        int net_id_offset = builder.createString(this.netObj.objectID);
//        int sync_vars_vector_offset = RegisterSyncVarsRequest.createSyncVarsVector(builder, sync_vars);
//        RegisterSyncVarsRequest.startRegisterSyncVarsRequest(builder);
//        RegisterSyncVarsRequest.addNetId(builder, net_id_offset);
//        RegisterSyncVarsRequest.addSyncVars(builder, sync_vars_vector_offset);
//        RegisterSyncVarsRequest.addIsDormant(builder, this.netObj.isDormant);
//        int request_offset = RegisterSyncVarsRequest.endRegisterSyncVarsRequest(builder);
//
//        int message_offset = Message.createMessage(builder, VariableMessage.RegisterSyncVarsRequest, request_offset);
//        builder.finish(message_offset);
////        System.out.println("syncvars added to payload");
//
//        byte[] buf = builder.sizedByteArray();
//        netObj.registerSyncVars(buf);
//    }
}


