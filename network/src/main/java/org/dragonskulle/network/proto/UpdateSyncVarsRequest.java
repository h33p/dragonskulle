// automatically generated by the FlatBuffers compiler, do not modify

package org.dragonskulle.network.proto;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class UpdateSyncVarsRequest extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_1_12_0(); }
  public static UpdateSyncVarsRequest getRootAsUpdateSyncVarsRequest(ByteBuffer _bb) { return getRootAsUpdateSyncVarsRequest(_bb, new UpdateSyncVarsRequest()); }
  public static UpdateSyncVarsRequest getRootAsUpdateSyncVarsRequest(ByteBuffer _bb, UpdateSyncVarsRequest obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public UpdateSyncVarsRequest __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String netId() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer netIdAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer netIdInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public org.dragonskulle.network.proto.ISyncVar syncVar() { return syncVar(new org.dragonskulle.network.proto.ISyncVar()); }
  public org.dragonskulle.network.proto.ISyncVar syncVar(org.dragonskulle.network.proto.ISyncVar obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createUpdateSyncVarsRequest(FlatBufferBuilder builder,
      int netIdOffset,
      int sync_varOffset) {
    builder.startTable(2);
    UpdateSyncVarsRequest.addSyncVar(builder, sync_varOffset);
    UpdateSyncVarsRequest.addNetId(builder, netIdOffset);
    return UpdateSyncVarsRequest.endUpdateSyncVarsRequest(builder);
  }

  public static void startUpdateSyncVarsRequest(FlatBufferBuilder builder) { builder.startTable(2); }
  public static void addNetId(FlatBufferBuilder builder, int netIdOffset) { builder.addOffset(0, netIdOffset, 0); }
  public static void addSyncVar(FlatBufferBuilder builder, int syncVarOffset) { builder.addOffset(1, syncVarOffset, 0); }
  public static int endUpdateSyncVarsRequest(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public UpdateSyncVarsRequest get(int j) { return get(new UpdateSyncVarsRequest(), j); }
    public UpdateSyncVarsRequest get(UpdateSyncVarsRequest obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}
