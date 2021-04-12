/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.network.components.sync.SyncString;

/** @author Oscar L The Capital Component. */
@Accessors(prefix = "m")
@Log
public class Capital extends NetworkableComponent {

    /** A syncable field. */
    @Getter private SyncBool mSyncMe = new SyncBool(false);
    /** A syncable field. */
    @Getter private SyncString mSyncMeAlso = new SyncString("Hello World");

    @Getter private final SyncInt mClientToggled = new SyncInt(0);

    /**
     * Creates the link between the request type @code{new AttackRequest()} and what to do when
     * invoked @code{this::handleEvent}.
     */
    public transient ClientRequest<TestAttackData> mPasswordRequest;

    public static final int CORRECT_PASSWORD = 4242;
    /* Used for testing */
    public static final int INCORRECT_PASSWORD = CORRECT_PASSWORD + 1;
    // Marked as transient since serializer can not serialize lambdas, which is very sad

    /** We need to initialize requests here, since java does not like to serialize lambdas. */
    @Override
    protected void onNetworkInitialize() {
        mPasswordRequest = new ClientRequest<>(new TestAttackData(), this::handleEvent);
    }
    /**
     * How this component will react to an attack event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(TestAttackData data) {
        if (data.mPassword == CORRECT_PASSWORD) {
            mClientToggled.set(data.mToBuilding);
        }
    }
    // The following functions are helpers to toggle the sync values

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

    @Override
    protected void onDestroy() {}
}
