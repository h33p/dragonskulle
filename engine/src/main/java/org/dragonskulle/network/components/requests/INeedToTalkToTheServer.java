/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import org.dragonskulle.network.ClientGameInstance;
import org.dragonskulle.network.components.ClientNetworkManager;

/**
 * @author Oscar L
 *     <p>Implement this if the component needs to talk to the server
 */
public interface INeedToTalkToTheServer {
    default void submitRequest(GameActionRequest request) {
        final ClientGameInstance.NetworkClientSendBytesCallback networkManager =
                ClientNetworkManager.getSendToServer();
        if (networkManager != null) {
            byte[] bytes = request.toBytes();
            networkManager.send(bytes);
        }
    }
}
