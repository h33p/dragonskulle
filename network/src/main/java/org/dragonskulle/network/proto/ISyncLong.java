/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class ISyncLong extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static ISyncLong getRootAsISyncLong(ByteBuffer _bb) {
        return getRootAsISyncLong(_bb, new ISyncLong());
    }

    public static ISyncLong getRootAsISyncLong(ByteBuffer _bb, ISyncLong obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public ISyncLong __assign(int _i, ByteBuffer _bb) {
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

    public long data() {
        int o = __offset(6);
        return o != 0 ? bb.getLong(o + bb_pos) : 0L;
    }

    public static int createISyncLong(FlatBufferBuilder builder, int idOffset, long data) {
        builder.startTable(2);
        ISyncLong.addData(builder, data);
        ISyncLong.addId(builder, idOffset);
        return ISyncLong.endISyncLong(builder);
    }

    public static void startISyncLong(FlatBufferBuilder builder) {
        builder.startTable(2);
    }

    public static void addId(FlatBufferBuilder builder, int idOffset) {
        builder.addOffset(0, idOffset, 0);
    }

    public static void addData(FlatBufferBuilder builder, long data) {
        builder.addLong(1, data, 0L);
    }

    public static int endISyncLong(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public ISyncLong get(int j) {
            return get(new ISyncLong(), j);
        }

        public ISyncLong get(ISyncLong obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
