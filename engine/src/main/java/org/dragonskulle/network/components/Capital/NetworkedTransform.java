/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.Capital;

import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;

/**
 * @author Oscar L A simple version of a networked transform, it moves the x value of the component
 *     forward and backwards
 */
public class NetworkedTransform extends NetworkableComponent implements IFrameUpdate {
    public SyncVector3 position = new SyncVector3(new Vector3f(0, 0, 0));
    int shouldFlipDirection = 1;

    boolean isServer = false;

    @Deprecated()
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
            float oldX = position.get().x;
            if (oldX > 0.5) {
                shouldFlipDirection = -1;
            } else if (oldX < -0.3) {
                shouldFlipDirection = 1;
            }
            getGameObject().getTransform().translate(shouldFlipDirection * (deltaTime), 0, 0);
            position.set(getGameObject().getTransform().getLocalPosition());
        } else {
            getGameObject().getTransform().setPosition(position.get());
        }
    }

    @Override
    public String toString() {
        return "NetworkedTransform{"
                + "position="
                + position
                + ", shouldFlipDirection="
                + shouldFlipDirection
                + ", isServer="
                + isServer
                + '}';
    }
}
