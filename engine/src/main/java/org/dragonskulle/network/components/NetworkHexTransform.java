package org.dragonskulle.network.components;

import org.dragonskulle.components.*;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class NetworkHexTransform extends NetworkableComponent implements IFixedUpdate, IOnAwake {
    public SyncVector3 mAxialCoordinate = new SyncVector3(new Vector3f(0, 0, 0));
    private TransformHex hexTransform;

    public NetworkHexTransform(int q, int r, int s) {
        mAxialCoordinate.set(new Vector3f(q, r, s));
    }

    public NetworkHexTransform() {
        mAxialCoordinate.set(new Vector3f(0, 0, 0));
    }

    @Override
    protected void onDestroy() {
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        setHexPosition();
    }

    private void setHexPosition() {
        NetworkObject netman = getNetworkObject();
        if (netman != null && hexTransform != null) {
            if (netman.isServer()) {
                Vector3f newPosition = hexTransform.getLocalPosition(new Vector3f());
                if (!mAxialCoordinate.get().equals(newPosition)) {
                    mAxialCoordinate.set(newPosition);
                }
            } else {
                hexTransform.setPosition(mAxialCoordinate.get());
            }
        }

    }

    public void setAxial(int q, int r, int s) {
        this.mAxialCoordinate.set(new Vector3f(q, r, s));
    }

    public void setAxial(Vector3f coordinates) {
        this.mAxialCoordinate.set(coordinates);
    }

    /**
     * Called when a component is first added to a scene to allow initial setup of variables
     */
    @Override
    public void onAwake() {
        hexTransform = getGameObject().getTransform(TransformHex.class);
        setHexPosition();
    }

    @Override
    public String toString() {
        return "NetworkHexTransform{" +
                "mAxialCoordinate=" + mAxialCoordinate +
                '}';
    }
}