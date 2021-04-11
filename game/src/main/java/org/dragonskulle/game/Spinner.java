/* (C) 2021 DragonSkulle */

package org.dragonskulle.game;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform3D;

/**
 * Simple spinning objects!.
 *
 * <p>Oh, they also shift in sine form
 *
 * @author Aurimas Blažulionis
 */
public class Spinner extends Component implements IOnAwake, IFrameUpdate {
    public float mSpinSpeed = 60.f;
    public float mSineSpeed = 50.f;
    public float mSineAmplitude = .5f;

    private Transform3D mTransform;
    private float mTotalTime = 0.f;

    public Spinner() {}

    public Spinner(float spinSpeed, float sineSpeed, float sineAmplitude) {
        this.mSpinSpeed = spinSpeed;
        this.mSineSpeed = sineSpeed;
        this.mSineAmplitude = sineAmplitude;
    }

    @Override
    public void onAwake() {
        mTransform = (Transform3D) getGameObject().getTransform();
    }

    @Override
    public void frameUpdate(float deltaTime) {
        mTransform.rotateDeg(0, 0, mSpinSpeed * deltaTime);

        float sineDelta = -(float) Math.sin(mTotalTime);
        mTotalTime += deltaTime;
        sineDelta += (float) Math.sin(mTotalTime);

        mTransform.translate(0, 0, sineDelta * mSineAmplitude);
    }

    @Override
    public void onDestroy() {}
}
