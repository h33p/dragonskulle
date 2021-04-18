/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * Allows server to send events to the clients.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class ServerEvent<T extends INetSerializable> {
    private IHandler<T> mHandler;
    private T mTmpData;
    @Getter private NetworkObject mNetworkObject;
    private int mEventId;
    @Getter private final EventRecipients mRecipients;
    @Getter private final EventTimeframe mTimeframe;

    /**
     * Describes who should receive the event.
     *
     * <p>{@code ACTIVE_CLIENTS} is the default that will send the event to all clients that would
     * generally see it {@code OWNER} sends an event only to the owner of the object {@code
     * ALL_CLIENTS} sends the event to all clients, even those who do not receive updates for this
     * object
     */
    public static enum EventRecipients {
        ACTIVE_CLIENTS((byte) 0),
        OWNER((byte) 1),
        ALL_CLIENTS((byte) 2);

        @Getter private final byte mValue;

        /**
         * Constructor.
         *
         * @param value the enum byte value
         */
        private EventRecipients(byte value) {
            mValue = value;
        }
    }

    /**
     * Describes the time frame of an event
     *
     * <p>{@code INSTANT} sends the event to all clients it can send to, instantly. If there are any
     * clients that do not receive updates for the object, they will not receive the event. This is
     * useful for short time events like attack wins, that happen quickly and do not have long-term
     * consequences. {@code LONG_TERM_DELAYABLE} will send the event to all clients it can send to,
     * but also store it, and relay it to all other clients that have not received this event when
     * they start receiving updates/join. This timeframe only makes sense on either {@code
     * ACTIVE_CLIENTS}, or {@code ALL_CLIENTS} recipients.
     */
    public static enum EventTimeframe {
        INSTANT((byte) 0),
        LONG_TERM_DELAYABLE((byte) 1);

        @Getter private final byte mValue;

        /**
         * Constructor.
         *
         * @param value the byte value
         */
        private EventTimeframe(byte value) {
            mValue = value;
        }
    }

    /**
     * The interface for settings the values in an server invokation.
     *
     * @param <T> the type parameter for the Invoke type
     */
    public static interface IInvokationSetter<T extends INetSerializable> {
        /**
         * Runs the invokation setter.
         *
         * @param data the data
         */
        void setValues(T data);
    }

    /**
     * Defines a server event to be executed on the clients.
     *
     * @param defaultValue the template of the event e.g {@code TestAttackData}
     * @param handler the handler for the event
     * @param recipients who should receive the event
     * @param timeframe controls the timeframe of event storage
     */
    public ServerEvent(
            T defaultValue,
            IHandler<T> handler,
            EventRecipients recipients,
            EventTimeframe timeframe) {
        mTmpData = defaultValue;
        mHandler = handler;
        mRecipients = recipients;
        mTimeframe = timeframe;
    }

    /**
     * Defines a server event to be executed on the clients.
     *
     * <p>This constructor defaults the event to be invoked on all active clients, instantly (with
     * no storage of the event).
     *
     * @param defaultValue the template of the event e.g {@code TestAttackData}
     * @param handler the handler for the event
     */
    public ServerEvent(T defaultValue, IHandler<T> handler) {
        this(defaultValue, handler, EventRecipients.ACTIVE_CLIENTS, EventTimeframe.INSTANT);
    }

    /**
     * Attaches a network object to an event Id.
     *
     * @param obj the obj
     * @param id the id
     */
    public void attachNetworkObject(NetworkObject obj, int id) {
        mNetworkObject = obj;
        mEventId = id;
    }

    /**
     * Invoke an event with a setter lambda
     *
     * <p>This method should be more efficient than passing {@code new} data every time, since it
     * would not invoke GC allocations (if Java is smart about it).
     *
     * @param setter setter interface for the temporary data
     */
    public void invoke(IInvokationSetter<T> setter) {
        setter.setValues(mTmpData);
        invoke(mTmpData);
    }

    /**
     * Invokes an event
     *
     * <p>This method sends an event to the clients, if the server is within the recipients, the
     * event gets called directly on it.
     *
     * @param data data to send/invoke.
     */
    public void invoke(T data) {
        try {
            if (!mNetworkObject.isServer()) {
                log.warning(
                        "Server event invoked on the client obj "
                                + mNetworkObject.getId()
                                + ". This is wrong!");
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DataOutputStream oos = new DataOutputStream(bos)) {
                oos.writeByte(NetworkConfig.Codes.MESSAGE_SERVER_EVENT);
                oos.writeInt(mNetworkObject.getId());
                oos.writeInt(mEventId);
                data.serialize(oos);
                oos.flush();
            }
            bos.flush();
            bos.close();

            ServerNetworkManager serverManager =
                    mNetworkObject.getNetworkManager().getServerManager();
            if (serverManager != null) {
                serverManager.sendEvent(this, bos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles an event with its corresponding invokation.
     *
     * @param inStream the data stream
     * @throws IOException thrown when an error occurs when reading from the stream {@code
     *     inStream}.
     */
    public void handle(DataInputStream inStream) throws IOException {
        mTmpData.deserialize(inStream);
        mHandler.invokeHandler(mTmpData);
    }
}
