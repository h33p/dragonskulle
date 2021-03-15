/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform3D;

/**
 * Simple spinning objects!
 *
 * <p>Oh, they also shift in sine form
 *
 * @author Aurimas Blažulionis
 */
public class Spinner extends Component implements IOnAwake, IFrameUpdate {
    public float spinSpeed = 60.f;
    public float sineSpeed = 50.f;
    public float sineAmplitude = .5f;

    private Transform3D mTransform;
    private float mTotalTime = 0.f;

    public Spinner() {}

    public Spinner(float spinSpeed, float sineSpeed, float sineAmplitude) {
        this.spinSpeed = spinSpeed;
        this.sineSpeed = sineSpeed;
        this.sineAmplitude = sineAmplitude;
    }

    @Override
    public void onAwake() {
        mTransform = (Transform3D) getGameObject().getTransform();
    }

    @Override
    public void frameUpdate(float deltaTime) {
        mTransform.rotateDeg(0, 0, spinSpeed * deltaTime);

        float sineDelta = -(float) Math.sin(mTotalTime);
        mTotalTime += deltaTime;
        sineDelta += (float) Math.sin(mTotalTime);

        mTransform.translate(0, 0, sineDelta * sineAmplitude);
    }

    @Override
    public void onDestroy() {}
}
