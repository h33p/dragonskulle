/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.Reference;

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

    private Reference<MenuFader> mFadeControls;

    /** Instantiates a new Spinner. */
    public Spinner() {}

    /**
     * Instantiates a new Spinner.
     *
     * @param spinSpeed the spin speed
     * @param sineSpeed the sine speed
     * @param sineAmplitude the sine amplitude
     */
    public Spinner(float spinSpeed, float sineSpeed, float sineAmplitude) {
        this.mSpinSpeed = spinSpeed;
        this.mSineSpeed = sineSpeed;
        this.mSineAmplitude = sineAmplitude;
    }

    @Override
    public void onAwake() {
        mTransform = (Transform3D) getGameObject().getTransform();
        mFadeControls = getGameObject().getComponent(MenuFader.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        mTransform.rotateDeg(0, 0, mSpinSpeed * deltaTime);

        float sineDelta = -(float) Math.sin(mTotalTime);
        mTotalTime += deltaTime;
        sineDelta += (float) Math.sin(mTotalTime);

        mTransform.translate(0, 0, sineDelta * mSineAmplitude);

        /*if (Reference.isValid(mFadeControls)) {
            mFadeControls.get().setFadeSpeed(-mSineSpeed);
        }*/
    }

    @Override
    public void onDestroy() {}
}
