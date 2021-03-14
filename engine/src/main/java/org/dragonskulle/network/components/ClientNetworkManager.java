/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.network.ClientGameInstance;
import org.dragonskulle.network.FixedUpdate;

/** @author Oscar L */
@Accessors(prefix = "m")
public class ClientNetworkManager extends NetworkManager {
    @Getter private static ClientGameInstance.NetworkClientSendBytesCallback mSendToServer = null;

    public ClientNetworkManager(
            FixedUpdate serverUpdateCallback,
            ClientGameInstance.NetworkClientSendBytesCallback sendBytesToServerCallback) {
        super(serverUpdateCallback);
        mSendToServer = sendBytesToServerCallback;
    }
}
