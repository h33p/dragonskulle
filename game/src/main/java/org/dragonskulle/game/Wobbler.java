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
    @Getter @Setter private float mWobbleSpeed = 1.f;
    @Getter @Setter private float mWobbleRange = 1.f;

    @Getter @Setter private boolean mCreateFadeControls = true;

    private Reference<MenuFader> mFadeControls;

    private TransformHex mTransform;
    @Getter @Setter private float mPhaseShift = 0f;
    private float mTotalTime = 0.f;

    public Wobbler() {}

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
