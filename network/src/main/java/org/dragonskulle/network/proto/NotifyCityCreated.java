/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class NotifyCityCreated extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static NotifyCityCreated getRootAsNotifyCityCreated(ByteBuffer _bb) {
        return getRootAsNotifyCityCreated(_bb, new NotifyCityCreated());
    }

    public static NotifyCityCreated getRootAsNotifyCityCreated(
            ByteBuffer _bb, NotifyCityCreated obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public NotifyCityCreated __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public int cityId() {
        int o = __offset(4);
        return o != 0 ? bb.getInt(o + bb_pos) : 0;
    }

    public org.dragonskulle.network.proto.AxialCoordinate coord() {
        return coord(new org.dragonskulle.network.proto.AxialCoordinate());
    }

    public org.dragonskulle.network.proto.AxialCoordinate coord(
            org.dragonskulle.network.proto.AxialCoordinate obj) {
        int o = __offset(6);
        return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null;
    }

    public static int createNotifyCityCreated(
            FlatBufferBuilder builder, int cityId, int coordOffset) {
        builder.startTable(2);
        NotifyCityCreated.addCoord(builder, coordOffset);
        NotifyCityCreated.addCityId(builder, cityId);
        return NotifyCityCreated.endNotifyCityCreated(builder);
    }

    public static void startNotifyCityCreated(FlatBufferBuilder builder) {
        builder.startTable(2);
    }

    public static void addCityId(FlatBufferBuilder builder, int cityId) {
        builder.addInt(0, cityId, 0);
    }

    public static void addCoord(FlatBufferBuilder builder, int coordOffset) {
        builder.addOffset(1, coordOffset, 0);
    }

    public static int endNotifyCityCreated(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public NotifyCityCreated get(int j) {
            return get(new NotifyCityCreated(), j);
        }

        public NotifyCityCreated get(NotifyCityCreated obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
