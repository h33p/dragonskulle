/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.ClientGameInstance;
import org.dragonskulle.network.NetworkConfig;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.components.ClientNetworkManager;
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

    public ClientRequest(T defaultValue, IRequestHandler<T> handler) {
        mTmpData = defaultValue;
        mHandler = handler;
    }

    public void attachNetworkObject(NetworkObject obj, int id) {
        mNetworkObject = obj;
        mRequestId = id;
    }

    public void invoke(T data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DataOutputStream oos = new DataOutputStream(bos)) {
                oos.writeInt(mNetworkObject.getId());
                oos.writeInt(mRequestId);
                data.serialize(oos);
                oos.flush();
            }
            bos.close();
            final ClientGameInstance.NetworkClientSendBytesCallback networkManager =
                    ClientNetworkManager.getSendToServer();
            networkManager.send(
                    NetworkMessage.build(
                            NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST, bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle(DataInputStream inStream) throws IOException {
        mTmpData.deserialize(inStream);
        mHandler.handleRequest(mTmpData);
    }
}
