/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Reference;

/**
 * Simple wobbly objects!.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Wobbler extends Component implements IOnAwake, IFrameUpdate {
    /** Wobble speed. */
    @Getter @Setter private float mWobbleSpeed = 1.f;
    /** Wobble range. */
    @Getter @Setter private float mWobbleRange = 1.f;

    /** Should this wobbler spawn fade controls. */
    @Getter @Setter private boolean mCreateFadeControls = true;

    /** Spawned fade controls. */
    private Reference<MenuFader> mFadeControls;

    /** Internal transform reference. */
    private TransformHex mTransform;

    /** Phase shift - shifts the math sine function time. */
    @Getter @Setter private float mPhaseShift = 0f;

    /** Current total time. */
    private float mTotalTime = 0.f;

    /** Default constructor for {@link Wobbler}. */
    public Wobbler() {}

    /**
     * Constructor for {@link Wobbler}.
     *
     * @param wobbleSpeed wobbling speed.
     * @param wobbleRange wobbling range.
     */
    public Wobbler(float wobbleSpeed, float wobbleRange) {
        this.mWobbleSpeed = wobbleSpeed;
        this.mWobbleRange = wobbleRange;
    }

    @Override
    public void onAwake() {
        mTransform = (TransformHex) getGameObject().getTransform();
        mTotalTime = mPhaseShift * mWobbleSpeed;

        mFadeControls = getGameObject().getComponent(MenuFader.class);

        if (mFadeControls == null && mCreateFadeControls) {
            mFadeControls = new MenuFader().getReference(MenuFader.class);
            getGameObject().addComponent(mFadeControls.get());
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        float sineDelta = -(float) Math.sin(mTotalTime);
        mTotalTime += deltaTime * mWobbleSpeed;
        sineDelta += (float) Math.sin(mTotalTime);

        mTransform.translate(sineDelta * mWobbleRange);

        if (Reference.isValid(mFadeControls)) {
            mFadeControls.get().setPhaseShift(mPhaseShift);
            mFadeControls.get().setAlphaMul(1f / (mPhaseShift * 3f + 1f));
        }
    }

    @Override
    public void onDestroy() {}
}
