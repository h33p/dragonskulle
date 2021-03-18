/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.components.*;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class NetworkHexTransform extends NetworkableComponent implements IFixedUpdate, IOnAwake {
    public SyncVector3 mAxialCoordinate = new SyncVector3(new Vector3f(0, 0, 0));
    private TransformHex hexTransform;

    public NetworkHexTransform(int q, int r) {
        mAxialCoordinate.set(new Vector3f(q, r, 0));
    }

    public NetworkHexTransform() {
        mAxialCoordinate.set(new Vector3f(0, 0, 0));
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        setHexPosition();
    }

    private void setHexPosition() {
        NetworkObject netman = getNetworkObject();
        if (netman != null && hexTransform != null) {
            if (netman.isServer()) {
                Vector3f newPosition = hexTransform.getLocalPosition(new Vector3f());
                newPosition.z = hexTransform.getHeight();
                if (!mAxialCoordinate.get().equals(newPosition) || true) {
                    mAxialCoordinate.set(newPosition);
                }
            } else {
                Vector3fc pos = mAxialCoordinate.get();
                hexTransform.setPosition(pos.x(), pos.y());
                hexTransform.setHeight(pos.z());
            }
        }
    }

    /** Called when a component is first added to a scene to allow initial setup of variables */
    @Override
    public void onAwake() {
        hexTransform = getGameObject().getTransform(TransformHex.class);
        setHexPosition();
    }

    @Override
    public String toString() {
        return "NetworkHexTransform{" + "mAxialCoordinate=" + mAxialCoordinate + '}';
    }
}
