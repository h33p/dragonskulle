/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.network.FixedUpdate;

/** @author Oscar L */
public class NetworkManager extends Component implements IFixedUpdate {
    private final FixedUpdate mUpdate;

    public NetworkManager(FixedUpdate serverUpdateCallback) {
        mUpdate = serverUpdateCallback;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        mUpdate.call();
    }
}
