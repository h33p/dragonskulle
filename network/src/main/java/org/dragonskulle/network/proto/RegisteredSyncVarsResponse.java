// automatically generated by the FlatBuffers compiler, do not modify

package org.dragonskulle.network.proto;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class RegisteredSyncVarsResponse extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_1_12_0(); }
  public static RegisteredSyncVarsResponse getRootAsRegisteredSyncVarsResponse(ByteBuffer _bb) { return getRootAsRegisteredSyncVarsResponse(_bb, new RegisteredSyncVarsResponse()); }
  public static RegisteredSyncVarsResponse getRootAsRegisteredSyncVarsResponse(ByteBuffer _bb, RegisteredSyncVarsResponse obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public RegisteredSyncVarsResponse __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public org.dragonskulle.network.proto.ISyncVar registered(int j) { return registered(new org.dragonskulle.network.proto.ISyncVar(), j); }
  public org.dragonskulle.network.proto.ISyncVar registered(org.dragonskulle.network.proto.ISyncVar obj, int j) { int o = __offset(4); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int registeredLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public org.dragonskulle.network.proto.ISyncVar.Vector registeredVector() { return registeredVector(new org.dragonskulle.network.proto.ISyncVar.Vector()); }
  public org.dragonskulle.network.proto.ISyncVar.Vector registeredVector(org.dragonskulle.network.proto.ISyncVar.Vector obj) { int o = __offset(4); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }

  public static int createRegisteredSyncVarsResponse(FlatBufferBuilder builder,
      int registeredOffset) {
    builder.startTable(1);
    RegisteredSyncVarsResponse.addRegistered(builder, registeredOffset);
    return RegisteredSyncVarsResponse.endRegisteredSyncVarsResponse(builder);
  }

  public static void startRegisteredSyncVarsResponse(FlatBufferBuilder builder) { builder.startTable(1); }
  public static void addRegistered(FlatBufferBuilder builder, int registeredOffset) { builder.addOffset(0, registeredOffset, 0); }
  public static int createRegisteredVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startRegisteredVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endRegisteredSyncVarsResponse(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public RegisteredSyncVarsResponse get(int j) { return get(new RegisteredSyncVarsResponse(), j); }
    public RegisteredSyncVarsResponse get(RegisteredSyncVarsResponse obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}
