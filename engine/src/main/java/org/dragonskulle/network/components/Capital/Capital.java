/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.Capital;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.requests.TestAttackData;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.network.components.sync.SyncString;

/** @author Oscar L The Capital Component. */
@Accessors(prefix = "m")
@Log
public class Capital extends NetworkableComponent implements TestAttackData.IEvent {

    /** A syncable field. */
    @Getter public SyncBool mSyncMe = new SyncBool(false);
    /** A syncable field. */
    @Getter public SyncString mSyncMeAlso = new SyncString("Hello World");

    @Getter public final SyncInt mClientToggled = new SyncInt(0);

    /**
     * Creates the link between the request type @code{new AttackRequest()} and what to do when
     * invoked @code{this::handleEvent}
     */
    private transient ClientRequest<TestAttackData> mPasswordRequest;

    public static final int CORRECT_PASSWORD = 4242;
    /** Used for testing */
    public static final int INCORRECT_PASSWORD = CORRECT_PASSWORD + 1;
    // Marked as transient since serializer can not serialize lambdas, which is very sad

    /** We need to initialize requests here, since java does not like to serialize lambdas */
    @Override
    protected void onNetworkInitialize() {
        mPasswordRequest = new ClientRequest<>(new TestAttackData(), this::handleEvent);
    }

    /**
     * How this component will react to an attack event.
     *
     * @param data attack event being executed on the server.
     */
    @Override
    public void handleEvent(TestAttackData data) {
        if (data.mPassword == CORRECT_PASSWORD) {
            mClientToggled.set(data.mToBuilding);
        }
    }

    /**
     * This is how the client will invoke the attack event.
     *
     * @param data this is the attack interface, containing a password value that will be checked on
     *     server use {@code CORRECT_PASSWORD} to pass the check. This is purely for testing and
     *     toBuilding, a building to "attack"
     */
    @Override
    public void clientInvokeEvent(TestAttackData data) {
        if (getNetworkObject().isServer()) {
            log.warning("Client invoke attack called on server! This is wrong!");
        } else {
            mPasswordRequest.invoke(data);
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
