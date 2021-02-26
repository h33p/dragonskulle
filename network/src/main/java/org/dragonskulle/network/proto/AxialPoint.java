/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class AxialPoint extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static AxialPoint getRootAsAxialPoint(ByteBuffer _bb) {
        return getRootAsAxialPoint(_bb, new AxialPoint());
    }

    public static AxialPoint getRootAsAxialPoint(ByteBuffer _bb, AxialPoint obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public AxialPoint __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public org.dragonskulle.network.proto.AxialCoordinate Value() {
        return Value(new org.dragonskulle.network.proto.AxialCoordinate());
    }

    public org.dragonskulle.network.proto.AxialCoordinate Value(
            org.dragonskulle.network.proto.AxialCoordinate obj) {
        int o = __offset(4);
        return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null;
    }

    public static int createAxialPoint(FlatBufferBuilder builder, int ValueOffset) {
        builder.startTable(1);
        AxialPoint.addValue(builder, ValueOffset);
        return AxialPoint.endAxialPoint(builder);
    }

    public static void startAxialPoint(FlatBufferBuilder builder) {
        builder.startTable(1);
    }

    public static void addValue(FlatBufferBuilder builder, int ValueOffset) {
        builder.addOffset(0, ValueOffset, 0);
    }

    public static int endAxialPoint(FlatBufferBuilder builder) {
        int o = builder.endTable();
        builder.required(o, 4); // Value
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public AxialPoint get(int j) {
            return get(new AxialPoint(), j);
        }

        public AxialPoint get(AxialPoint obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
