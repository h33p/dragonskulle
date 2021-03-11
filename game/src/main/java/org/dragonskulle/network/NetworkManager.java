/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;

/** @author Oscar L */
public class NetworkManager extends Component implements IFixedUpdate {
    Server.FixedUpdate update;

    public NetworkManager(Server.FixedUpdate serverUpdateCallback) {
        update = serverUpdateCallback;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        update.call();
    }
}
