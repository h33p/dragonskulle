/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.network.FixedUpdate;

/** @author Oscar L */
public class NetworkManager extends Component implements IFixedUpdate {
    FixedUpdate update;

    public NetworkManager(FixedUpdate serverUpdateCallback) {
        update = serverUpdateCallback;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        update.call();
    }
}
