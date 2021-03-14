/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.Capital;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.INetSerializable;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.network.components.sync.SyncString;

/** @author Oscar L The Capital Component. */
@Accessors(prefix = "m")
@Log
public class Capital extends NetworkableComponent {

    /** A syncable field. */
    @Getter public SyncBool mSyncMe = new SyncBool(false);
    /** A syncable field. */
    @Getter public SyncString mSyncMeAlso = new SyncString("Hello World");

    @Getter public final SyncInt mClientToggled = new SyncInt(0);

    public static final int CORRECT_PASSWORD = 4242;
    /** Used for testing */
    public static final int INCORRECT_PASSWORD = CORRECT_PASSWORD + 1;

    private static final class AttackData implements INetSerializable {
        int mPassword;
        int mToBuilding;

        private AttackData() {}

        public AttackData(int password, int toBuilding) {
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
    };

    // Marked as transient since serializer can not serialize lambdas, which is very sad
    private transient ClientRequest<AttackData> mPasswordRequest;

    /** We need to initialize requests here, since java does not like to serialize lambdas */
    @Override
    protected void onNetworkInitialize() {
        mPasswordRequest = new ClientRequest<>(new AttackData(), this::handleAttack);
    }

    /** Server-side attack handler. Can also be a lambda */
    public void handleAttack(AttackData data) {
        if (data.mPassword == CORRECT_PASSWORD) {
            mClientToggled.set(data.mToBuilding);
        }
    }

    /**
     * Modifies the boolean sync me.
     *
     * @param val the val
     */
    public void setBooleanSyncMe(boolean val) {
        this.mSyncMe.set(val);
    }

    /**
     * Modifies the string sync me also.
     *
     * @param val the val
     */
    public void setStringSyncMeAlso(String val) {
        this.mSyncMeAlso.set(val);
    }

    /**
     * Invoke a server event
     *
     * @param password a password value that will be checked on server use {@code CORRECT_PASSWORD}
     *     to pass the check. This is purely for testing
     * @param toBuilding building to "attack"
     */
    public void clientInvokeAttack(int password, int toBuilding) {
        if (getNetworkObject().isServer())
            log.warning("Client invoke attack called on server! This is wrong!");
        else mPasswordRequest.invoke(new AttackData(password, toBuilding));
    }

    @Override
    protected void onDestroy() {}
}
