/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.dragonskulle.network.components.sync.INetSerializable;

public final class TestAttackData implements INetSerializable {
    public interface IEvent extends INeedToTalkToTheServer<TestAttackData> {}

    public int mPassword;
    public int mToBuilding;

    public TestAttackData() {}

    public TestAttackData(int password, int toBuilding) {
        mPassword = password;
        mToBuilding = toBuilding;
    }

    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mPassword);
        stream.writeInt(mToBuilding);
    }

    public void deserialize(DataInputStream stream) throws IOException {
        mPassword = stream.readInt();
        mToBuilding = stream.readInt();
    }
}
