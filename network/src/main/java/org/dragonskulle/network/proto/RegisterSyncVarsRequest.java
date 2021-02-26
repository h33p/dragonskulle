/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class RegisterSyncVarsRequest extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static RegisterSyncVarsRequest getRootAsRegisterSyncVarsRequest(ByteBuffer _bb) {
        return getRootAsRegisterSyncVarsRequest(_bb, new RegisterSyncVarsRequest());
    }

    public static RegisterSyncVarsRequest getRootAsRegisterSyncVarsRequest(
            ByteBuffer _bb, RegisterSyncVarsRequest obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public RegisterSyncVarsRequest __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public String netId() {
        int o = __offset(4);
        return o != 0 ? __string(o + bb_pos) : null;
    }

    public ByteBuffer netIdAsByteBuffer() {
        return __vector_as_bytebuffer(4, 1);
    }

    public ByteBuffer netIdInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 4, 1);
    }

    public org.dragonskulle.network.proto.ISyncVar syncVars(int j) {
        return syncVars(new org.dragonskulle.network.proto.ISyncVar(), j);
    }

    public org.dragonskulle.network.proto.ISyncVar syncVars(
            org.dragonskulle.network.proto.ISyncVar obj, int j) {
        int o = __offset(6);
        return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null;
    }

    public int syncVarsLength() {
        int o = __offset(6);
        return o != 0 ? __vector_len(o) : 0;
    }

    public org.dragonskulle.network.proto.ISyncVar.Vector syncVarsVector() {
        return syncVarsVector(new org.dragonskulle.network.proto.ISyncVar.Vector());
    }

    public org.dragonskulle.network.proto.ISyncVar.Vector syncVarsVector(
            org.dragonskulle.network.proto.ISyncVar.Vector obj) {
        int o = __offset(6);
        return o != 0 ? obj.__assign(__vector(o), 4, bb) : null;
    }

    public boolean isDormant() {
        int o = __offset(8);
        return o != 0 ? 0 != bb.get(o + bb_pos) : false;
    }

    public static int createRegisterSyncVarsRequest(
            FlatBufferBuilder builder, int netIdOffset, int sync_varsOffset, boolean is_dormant) {
        builder.startTable(3);
        RegisterSyncVarsRequest.addSyncVars(builder, sync_varsOffset);
        RegisterSyncVarsRequest.addNetId(builder, netIdOffset);
        RegisterSyncVarsRequest.addIsDormant(builder, is_dormant);
        return RegisterSyncVarsRequest.endRegisterSyncVarsRequest(builder);
    }

    public static void startRegisterSyncVarsRequest(FlatBufferBuilder builder) {
        builder.startTable(3);
    }

    public static void addNetId(FlatBufferBuilder builder, int netIdOffset) {
        builder.addOffset(0, netIdOffset, 0);
    }

    public static void addSyncVars(FlatBufferBuilder builder, int syncVarsOffset) {
        builder.addOffset(1, syncVarsOffset, 0);
    }

    public static int createSyncVarsVector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]);
        return builder.endVector();
    }

    public static void startSyncVarsVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(4, numElems, 4);
    }

    public static void addIsDormant(FlatBufferBuilder builder, boolean isDormant) {
        builder.addBoolean(2, isDormant, false);
    }

    public static int endRegisterSyncVarsRequest(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public RegisterSyncVarsRequest get(int j) {
            return get(new RegisterSyncVarsRequest(), j);
        }

        public RegisterSyncVarsRequest get(RegisterSyncVarsRequest obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
