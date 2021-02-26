/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

import com.google.flatbuffers.*;
import java.nio.*;
import java.util.*;

@SuppressWarnings("unused")
public final class Message extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_1_12_0();
    }

    public static Message getRootAsMessage(ByteBuffer _bb) {
        return getRootAsMessage(_bb, new Message());
    }

    public static Message getRootAsMessage(ByteBuffer _bb, Message obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public Message __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public byte contentsType() {
        int o = __offset(4);
        return o != 0 ? bb.get(o + bb_pos) : 0;
    }

    public Table contents(Table obj) {
        int o = __offset(6);
        return o != 0 ? __union(obj, o + bb_pos) : null;
    }

    public static int createMessage(
            FlatBufferBuilder builder, byte contents_type, int contentsOffset) {
        builder.startTable(2);
        Message.addContents(builder, contentsOffset);
        Message.addContentsType(builder, contents_type);
        return Message.endMessage(builder);
    }

    public static void startMessage(FlatBufferBuilder builder) {
        builder.startTable(2);
    }

    public static void addContentsType(FlatBufferBuilder builder, byte contentsType) {
        builder.addByte(0, contentsType, 0);
    }

    public static void addContents(FlatBufferBuilder builder, int contentsOffset) {
        builder.addOffset(1, contentsOffset, 0);
    }

    public static int endMessage(FlatBufferBuilder builder) {
        int o = builder.endTable();
        return o;
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public Message get(int j) {
            return get(new Message(), j);
        }

        public Message get(Message obj, int j) {
            return obj.__assign(__indirect(__element(j), bb), bb);
        }
    }
}
