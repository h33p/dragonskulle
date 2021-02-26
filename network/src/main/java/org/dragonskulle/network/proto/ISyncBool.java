/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class ISyncBool extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static ISyncBool getRootAsISyncBool(ByteBuffer _bb) {
        return getRootAsISyncBool(_bb, new ISyncBool());
    }

    public static ISyncBool getRootAsISyncBool(ByteBuffer _bb, ISyncBool obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public ISyncBool __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public String id() {
        int o = __offset(4);
        return o != 0 ? __string(o + bb_pos) : null;
    }

    public ByteBuffer idAsByteBuffer() {
        return __vector_as_bytebuffer(4, 1);
    }

    public ByteBuffer idInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 4, 1);
    }

    public boolean data() {
        int o = __offset(6);
        return o != 0 ? 0 != bb.get(o + bb_pos) : false;
    }

    public static int createISyncBool(FlatBufferBuilder builder, int idOffset, boolean data) {
        builder.startTable(2);
        ISyncBool.addId(builder, idOffset);
        ISyncBool.addData(builder, data);
        return ISyncBool.endISyncBool(builder);
    }

    public static void startISyncBool(FlatBufferBuilder builder) {
        builder.startTable(2);
    }

    public static void addId(FlatBufferBuilder builder, int idOffset) {
        builder.addOffset(0, idOffset, 0);
    }

    public static void addData(FlatBufferBuilder builder, boolean data) {
        builder.addBoolean(1, data, false);
    }

    public static int endISyncBool(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public ISyncBool get(int j) {
            return get(new ISyncBool(), j);
        }

        public ISyncBool get(ISyncBool obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
