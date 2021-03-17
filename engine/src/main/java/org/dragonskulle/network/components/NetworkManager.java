/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.network.DisposingMethod;
import org.dragonskulle.network.FixedUpdate;

/** @author Oscar L */
public class NetworkManager extends Component implements IFixedUpdate {
    private final FixedUpdate mUpdate;
    private final DisposingMethod dispose;

    public NetworkManager(FixedUpdate serverUpdateCallback, DisposingMethod dispose) {
        mUpdate = serverUpdateCallback;
        this.dispose = dispose;
    }

    @Override
    protected void onDestroy() {
        this.dispose.call();
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        mUpdate.call();
    }
}
