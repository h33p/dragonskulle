//package org.dragonskulle.network.flatbuffers;
//
//import com.google.flatbuffers.FlatBufferBuilder;
//import org.dragonskulle.network.components.sync.ISyncVar;
//import org.dragonskulle.network.components.sync.SyncBool;
//import org.dragonskulle.network.components.sync.SyncString;
//import org.dragonskulle.network.proto.*;
//
//public class FlatBufferHelpers {
//    public static int ISyncVar2flatb(FlatBufferBuilder builder, ISyncVar sv) { //deal with abstract sync
//        byte sync_type = AnyISyncVar.NONE;
//        int syncvar_offset = -1;
//        Class<?> clazz = sv.getClass();
//        int built_offset;
//        if (clazz.isAssignableFrom(SyncBool.class)) {
//            sync_type = AnyISyncVar.ISyncBool;
//            built_offset = builder.createString(sv.getId());
//            syncvar_offset = ISyncBool.createISyncBool(builder, built_offset, (boolean) sv.looselyGet());
//        } else if (clazz.isAssignableFrom(SyncString.class)) {
//            sync_type = AnyISyncVar.ISyncString;
//            built_offset = builder.createString(sv.getId());
//            int string_offset = builder.createString((String) sv.looselyGet());
//            syncvar_offset = ISyncString.createISyncString(builder, built_offset, string_offset);
//        } else if (clazz.isAssignableFrom(SyncLong.class)) {
//            sync_type = AnyISyncVar.ISyncLong;
//            built_offset = builder.createString(sv.getId());
//            syncvar_offset = ISyncLong.createISyncLong(builder, built_offset, (long) sv.looselyGet());
//        } else if (clazz.isAssignableFrom(SyncInt.class)) {
//            sync_type = AnyISyncVar.ISyncInt;
//            built_offset = builder.createString(sv.getId());
//            syncvar_offset = ISyncInt.createISyncInt(builder, built_offset, (int) sv.looselyGet());
//        } else if (clazz.isAssignableFrom(SyncFloat.class)) {
//            sync_type = AnyISyncVar.ISyncFloat;
//            built_offset = builder.createString(sv.getId());
//            syncvar_offset = ISyncFloat.createISyncFloat(builder, built_offset, (float) sv.looselyGet());
//        } else {
//            System.out.println("Sync type is not one of syncs. = " + clazz); //AbstractSync$1
//            System.out.println("isinstance of string? :: " + (sv instanceof SyncString));
//            System.out.println("super Info = " + clazz.getSuperclass()); //AbstractSync
//        }
//        //create single sync var to add to array
//        return org.dragonskulle.network.proto.ISyncVar.createISyncVar(builder, sync_type, syncvar_offset);
//    }
//
//    public static ISyncVar flatb2ISyncVar(org.dragonskulle.network.proto.ISyncVar syncVar) {
//        byte var_type = syncVar.syncVarType();
//        if (var_type == AnyISyncVar.ISyncBool) {
//            ISyncBool extracted_syncvar = (ISyncBool) syncVar.syncVar(new ISyncBool());
//            assert extracted_syncvar != null;
//            return new SyncBool(extracted_syncvar.id(), extracted_syncvar.data());
//        } else if (var_type == AnyISyncVar.ISyncFloat) {
//            ISyncFloat extracted_syncvar = (ISyncFloat) syncVar.syncVar(new ISyncFloat());
//            assert extracted_syncvar != null;
//            return new SyncFloat(extracted_syncvar.id(), extracted_syncvar.data());
//        } else if (var_type == AnyISyncVar.ISyncInt) {
//            ISyncInt extracted_syncvar = (ISyncInt) syncVar.syncVar(new ISyncInt());
//            assert extracted_syncvar != null;
//            return new SyncInt(extracted_syncvar.id(), extracted_syncvar.data());
//        } else if (var_type == AnyISyncVar.ISyncLong) {
//            ISyncLong extracted_syncvar = (ISyncLong) syncVar.syncVar(new ISyncLong());
//            assert extracted_syncvar != null;
//            return new SyncLong(extracted_syncvar.id(), extracted_syncvar.data());
//        } else if (var_type == AnyISyncVar.ISyncString) {
//            ISyncString extracted_syncvar = (ISyncString) syncVar.syncVar(new ISyncString());
//            assert extracted_syncvar != null;
//            return new SyncString(extracted_syncvar.id(), extracted_syncvar.data());
//        } else {
//            System.out.println("Could not convert to ISyncVar as not a valid type");
//            return null;
//        }
//
//    }
//
//}
