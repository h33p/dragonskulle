/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class ISyncString extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static ISyncString getRootAsISyncString(ByteBuffer _bb) {
        return getRootAsISyncString(_bb, new ISyncString());
    }

    public static ISyncString getRootAsISyncString(ByteBuffer _bb, ISyncString obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public ISyncString __assign(int _i, ByteBuffer _bb) {
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

    public String data() {
        int o = __offset(6);
        return o != 0 ? __string(o + bb_pos) : null;
    }

    public ByteBuffer dataAsByteBuffer() {
        return __vector_as_bytebuffer(6, 1);
    }

    public ByteBuffer dataInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 6, 1);
    }

    public static int createISyncString(FlatBufferBuilder builder, int idOffset, int dataOffset) {
        builder.startTable(2);
        ISyncString.addData(builder, dataOffset);
        ISyncString.addId(builder, idOffset);
        return ISyncString.endISyncString(builder);
    }

    public static void startISyncString(FlatBufferBuilder builder) {
        builder.startTable(2);
    }

    public static void addId(FlatBufferBuilder builder, int idOffset) {
        builder.addOffset(0, idOffset, 0);
    }

    public static void addData(FlatBufferBuilder builder, int dataOffset) {
        builder.addOffset(1, dataOffset, 0);
    }

    public static int endISyncString(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public ISyncString get(int j) {
            return get(new ISyncString(), j);
        }

        public ISyncString get(ISyncString obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
