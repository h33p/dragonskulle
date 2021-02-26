/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class UpdateSyncVarsResponse extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static UpdateSyncVarsResponse getRootAsUpdateSyncVarsResponse(ByteBuffer _bb) {
        return getRootAsUpdateSyncVarsResponse(_bb, new UpdateSyncVarsResponse());
    }

    public static UpdateSyncVarsResponse getRootAsUpdateSyncVarsResponse(
            ByteBuffer _bb, UpdateSyncVarsResponse obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public UpdateSyncVarsResponse __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public org.dragonskulle.network.proto.ISyncVar syncVar() {
        return syncVar(new org.dragonskulle.network.proto.ISyncVar());
    }

    public org.dragonskulle.network.proto.ISyncVar syncVar(
            org.dragonskulle.network.proto.ISyncVar obj) {
        int o = __offset(4);
        return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null;
    }

    public static int createUpdateSyncVarsResponse(FlatBufferBuilder builder, int sync_varOffset) {
        builder.startTable(1);
        UpdateSyncVarsResponse.addSyncVar(builder, sync_varOffset);
        return UpdateSyncVarsResponse.endUpdateSyncVarsResponse(builder);
    }

    public static void startUpdateSyncVarsResponse(FlatBufferBuilder builder) {
        builder.startTable(1);
    }

    public static void addSyncVar(FlatBufferBuilder builder, int syncVarOffset) {
        builder.addOffset(0, syncVarOffset, 0);
    }

    public static int endUpdateSyncVarsResponse(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public UpdateSyncVarsResponse get(int j) {
            return get(new UpdateSyncVarsResponse(), j);
        }

        public UpdateSyncVarsResponse get(UpdateSyncVarsResponse obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
