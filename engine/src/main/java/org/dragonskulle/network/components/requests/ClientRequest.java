/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * Allows client to request action from server
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class ClientRequest<T extends INetSerializable> {
    private IRequestHandler<T> mHandler;
    private T mTmpData;
    private NetworkObject mNetworkObject;
    private int mRequestId;

    public static interface IInvokationSetter<T extends INetSerializable> {
        void setValues(T data);
    }

    /**
     * Defined how an event it to be handled on the server.
     *
     * @param defaultValue the template of the event e.g {@code TestAttackData}
     * @param handler the handler for the event
     */
    public ClientRequest(T defaultValue, IRequestHandler<T> handler) {
        mTmpData = defaultValue;
        mHandler = handler;
    }

    public void attachNetworkObject(NetworkObject obj, int id) {
        mNetworkObject = obj;
        mRequestId = id;
    }

    /**
     * Invoke a request with a setter lambda
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
     * Invokes a request
     *
     * <p>This method sends a request to the server, if the object is owned by the player, or calls
     * it directly, if was invoked by the server.
     *
     * @param data data to send/invoke.
     */
    public void invoke(T data) {
        try {
            if (!mNetworkObject.isMine()) {
                log.warning(
                        "Invoked "
                                + data.getClass().getName()
                                + "event called on non-owned object! This is wrong!");
            } else if (mNetworkObject.isServer()) {
                mHandler.handleRequest(data);
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (DataOutputStream oos = new DataOutputStream(bos)) {
                    oos.writeInt(mNetworkObject.getId());
                    oos.writeInt(mRequestId);
                    data.serialize(oos);
                    oos.flush();
                }
                bos.close();
                mNetworkObject
                        .getNetworkManager()
                        .getClientManager()
                        .sendToServer(
                                NetworkMessage.build(
                                        NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST,
                                        bos.toByteArray()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle(DataInputStream inStream) throws IOException {
        mTmpData.deserialize(inStream);
        mHandler.handleRequest(mTmpData);
    }
}
