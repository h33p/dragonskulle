/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.dragonskulle.network.components.sync.INetSerializable;

public final class TestAttackData implements INetSerializable {
    public int mPassword;
    public int mToBuilding;

    public TestAttackData() {}

    public TestAttackData(int password, int toBuilding) {
        mPassword = password;
        mToBuilding = toBuilding;
    }

    public void serialize(DataOutput stream, int clientId) throws IOException {
        stream.writeInt(mPassword);
        stream.writeInt(mToBuilding);
    }

    public void deserialize(DataInput stream) throws IOException {
        mPassword = stream.readInt();
        mToBuilding = stream.readInt();
    }
}
