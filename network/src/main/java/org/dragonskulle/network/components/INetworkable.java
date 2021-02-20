package org.dragonskulle.network.components;

import com.google.flatbuffers.FlatBufferBuilder;
import org.dragonskulle.network.proto.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class INetworkable {
    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */

    final NetworkObject netObj;

    INetworkable(NetworkObject object) {
        this.netObj = object;
        System.out.println("netObj is assigned");
    }

    void dispose() {
        this.netObj.dispose();
    }

    void connectSyncVars() throws IllegalAccessException {
        List<Field> fields = Arrays.stream(this.getClass().getDeclaredFields()).filter(field -> ISyncVar.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
        int[] sync_vars = new int[fields.size()];
        int i = 0;
        System.out.println("No. Fields: " + fields.size());
        FlatBufferBuilder builder = new FlatBufferBuilder(512);
        int built_offset;


        for (Field f : fields) {
            ISyncVar sv = (ISyncVar) f.get(this); //retrieves syncvar from child and adds it to the connection array
            byte sync_type = AnyISyncVar.NONE;
            int syncvar_offset = -1;
            Class<?> clazz = sv.getClass();
            if (clazz == SyncBool.class) {
                sync_type = AnyISyncVar.ISyncBool;
                built_offset = builder.createString(sv.id);
                syncvar_offset = ISyncBool.createISyncBool(builder, built_offset, (boolean) sv.data);
            } else if (clazz == SyncString.class) {
                sync_type = AnyISyncVar.ISyncString;
                built_offset = builder.createString(sv.id);
                int string_offset = builder.createString((String) sv.data);
                syncvar_offset = ISyncString.createISyncString(builder, built_offset, string_offset);
            } else if (clazz == SyncLong.class) {
                sync_type = AnyISyncVar.ISyncLong;
                built_offset = builder.createString(sv.id);
                syncvar_offset = ISyncLong.createISyncLong(builder, built_offset, (long) sv.data);
            } else if (clazz == SyncInt.class) {
                sync_type = AnyISyncVar.ISyncInt;
                built_offset = builder.createString(sv.id);
                syncvar_offset = ISyncInt.createISyncInt(builder, built_offset, (int) sv.data);
            } else if (clazz == SyncFloat.class) {
                sync_type = AnyISyncVar.ISyncFloat;
                built_offset = builder.createString(sv.id);
                syncvar_offset = ISyncFloat.createISyncFloat(builder, built_offset, (float) sv.data);
            } else {
                System.out.println("Sync type is not one of syncs. = " + clazz);
            }
            //create single sync var to add to array
            int sync_var_id = org.dragonskulle.network.proto.ISyncVar.createISyncVar(builder, sync_type, syncvar_offset);
            sync_vars[i++] = sync_var_id;
            System.out.println("adding " + f.getName());
        }
        //need to add isdormant, array of syncvars to request.
        int net_id_offset = builder.createString(this.netObj.netID);
        int sync_vars_vector_offset = RegisterSyncVarsRequest.createSyncVarsVector(builder, sync_vars);
        RegisterSyncVarsRequest.startRegisterSyncVarsRequest(builder);
        RegisterSyncVarsRequest.addNetId(builder, net_id_offset);
        RegisterSyncVarsRequest.addSyncVars(builder, sync_vars_vector_offset);
        RegisterSyncVarsRequest.addIsDormant(builder, this.netObj.isDormant);
        int request_offset = RegisterSyncVarsRequest.endRegisterSyncVarsRequest(builder);
        builder.finish(request_offset);
        byte[] sync_vars_buf = builder.sizedByteArray();
        System.out.println("syncvars added to payload");
        netObj.registerSyncVars(sync_vars_buf);
    }
}


