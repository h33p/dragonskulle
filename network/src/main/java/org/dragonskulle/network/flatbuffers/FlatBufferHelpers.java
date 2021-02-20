package org.dragonskulle.network.flatbuffers;

import com.google.flatbuffers.FlatBufferBuilder;
import org.dragonskulle.network.components.*;
import org.dragonskulle.network.components.ISyncVar;
import org.dragonskulle.network.proto.*;

public class FlatBufferHelpers {
    public static int ISyncVar2flatb(FlatBufferBuilder builder, ISyncVar sv) {
        byte sync_type = AnyISyncVar.NONE;
        int syncvar_offset = -1;
        Class<?> clazz = sv.getClass();
        int built_offset;
        if (clazz == SyncBool.class) {
            sync_type = AnyISyncVar.ISyncBool;
            built_offset = builder.createString(sv.getId());
            syncvar_offset = ISyncBool.createISyncBool(builder, built_offset, (boolean) sv.get());
        } else if (clazz == SyncString.class) {
            sync_type = AnyISyncVar.ISyncString;
            built_offset = builder.createString(sv.getId());
            int string_offset = builder.createString((String) sv.get());
            syncvar_offset = ISyncString.createISyncString(builder, built_offset, string_offset);
        } else if (clazz == SyncLong.class) {
            sync_type = AnyISyncVar.ISyncLong;
            built_offset = builder.createString(sv.getId());
            syncvar_offset = ISyncLong.createISyncLong(builder, built_offset, (long) sv.get());
        } else if (clazz == SyncInt.class) {
            sync_type = AnyISyncVar.ISyncInt;
            built_offset = builder.createString(sv.getId());
            syncvar_offset = ISyncInt.createISyncInt(builder, built_offset, (int) sv.get());
        } else if (clazz == SyncFloat.class) {
            sync_type = AnyISyncVar.ISyncFloat;
            built_offset = builder.createString(sv.getId());
            syncvar_offset = ISyncFloat.createISyncFloat(builder, built_offset, (float) sv.get());
        } else {
            System.out.println("Sync type is not one of syncs. = " + clazz);
        }
        //create single sync var to add to array
        return org.dragonskulle.network.proto.ISyncVar.createISyncVar(builder, sync_type, syncvar_offset);
    }

    public static ISyncVar flatb2ISyncVar(org.dragonskulle.network.proto.ISyncVar syncVar) {
        byte var_type = syncVar.syncVarType();
        if (var_type == AnyISyncVar.ISyncBool) {
            ISyncBool extracted_syncvar = (ISyncBool) syncVar.syncVar(new ISyncBool());
            assert extracted_syncvar != null;
            return new SyncBool(extracted_syncvar.id(), extracted_syncvar.data());
        } else if (var_type == AnyISyncVar.ISyncFloat) {
            ISyncFloat extracted_syncvar = (ISyncFloat) syncVar.syncVar(new ISyncFloat());
            assert extracted_syncvar != null;
            return new SyncFloat(extracted_syncvar.id(), extracted_syncvar.data());
        } else if (var_type == AnyISyncVar.ISyncInt) {
            ISyncInt extracted_syncvar = (ISyncInt) syncVar.syncVar(new ISyncInt());
            assert extracted_syncvar != null;
            return new SyncInt(extracted_syncvar.id(), extracted_syncvar.data());
        } else if (var_type == AnyISyncVar.ISyncLong) {
            ISyncLong extracted_syncvar = (ISyncLong) syncVar.syncVar(new ISyncLong());
            assert extracted_syncvar != null;
            return new SyncLong(extracted_syncvar.id(), extracted_syncvar.data());
        } else if (var_type == AnyISyncVar.ISyncString) {
            ISyncString extracted_syncvar = (ISyncString) syncVar.syncVar(new ISyncString());
            assert extracted_syncvar != null;
            return new SyncString(extracted_syncvar.id(), extracted_syncvar.data());
        } else {
            System.out.println("Could not convert to ISyncVar as not a valid type");
            return null;
        }

    }
}
