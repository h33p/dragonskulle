/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * Allows server to send events to the clients.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class ServerRequest<T extends INetSerializable> {
    private IHandler<T> mHandler;
    private T mTmpData;
    private NetworkObject mNetworkObject;
    private int mRequestId;

    /**
     * Defined how an event it to be handled on the server.
     *
     * @param defaultValue the template of the event e.g {@code TestAttackData}.
     * @param handler the handler for the event.
     */
    public ServerRequest(T defaultValue, IHandler<T> handler) {
        mTmpData = defaultValue;
        mHandler = handler;
    }

    /**
     * Attach network object to the event.
     *
     * @param obj network object to attach.
     * @param id request's allocated ID.
     */
    public void attachNetworkObject(NetworkObject obj, int id) {
        mNetworkObject = obj;
        mRequestId = id;
    }

    /**
     * Invoke a request with a setter lambda.
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
     * Invokes a request.
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
                mHandler.invokeHandler(data);
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (DataOutputStream oos = new DataOutputStream(bos)) {
                    oos.writeByte(NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST);
                    oos.writeInt(mNetworkObject.getId());
                    oos.writeInt(mRequestId);
                    data.serialize(oos, -1);
                    oos.flush();
                }
                bos.close();
                mNetworkObject
                        .getNetworkManager()
                        .getClientManager()
                        .sendToServer(bos.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserialize and handle the event.
     *
     * <p>This method accepts an input stream (with request ID already parsed), and invokes the
     * event handler.
     *
     * @param inStream input stream.
     * @throws IOException if there is a stream or parsing error.
     */
    public void handle(DataInput inStream) throws IOException {
        mTmpData.deserialize(inStream);
        mHandler.invokeHandler(mTmpData);
    }
}
