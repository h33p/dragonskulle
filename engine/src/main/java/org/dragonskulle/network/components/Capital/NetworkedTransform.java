/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.Capital;

import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncFloat;

/** @author Oscar L */
public class NetworkedTransform extends NetworkableComponent implements IFrameUpdate {
    public SyncFloat x = new SyncFloat(0f);
    public SyncFloat y = new SyncFloat(0f);

    int shouldFlipDirection = 1;

    boolean isServer = false;

    public NetworkedTransform() {
        super();
    }

    public NetworkedTransform(int ownerId, int componentId, boolean isServer) {
        super(ownerId, componentId);
        this.isServer = isServer;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
        if (isServer) {
            float newX = x.get() + shouldFlipDirection * (0.001f * deltaTime);
            if (newX > 0.0005) {
                shouldFlipDirection = -1;
            } else if (newX < -0.0035) {
                shouldFlipDirection = 1;
            }
            this.x.set(newX);
            getGameObject().getTransform().translate(shouldFlipDirection * newX, 0, 0);
        }
    }

    @Override
    public String toString() {
        return "NetworkedTransform{"
                + "x="
                + x
                + ", y="
                + y
                + ", shouldFlipDirection="
                + shouldFlipDirection
                + ", isServer="
                + isServer
                + '}';
    }
}
