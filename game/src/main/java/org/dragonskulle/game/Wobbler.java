/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;

/**
 * Simple wobbly objects!
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Wobbler extends Component implements IOnAwake, IFrameUpdate {
    public float wobbleSpeed = 1.f;
    public float wobbleRange = 1.f;

    private TransformHex mTransform;
    @Getter @Setter private float mPhaseShift = 0f;
    private float mTotalTime = 0.f;

    public Wobbler() {}

    public Wobbler(float wobbleSpeed, float wobbleRange) {
        this.wobbleSpeed = wobbleSpeed;
        this.wobbleRange = wobbleRange;
    }

    @Override
    public void onAwake() {
        mTransform = (TransformHex) getGameObject().getTransform();
        mTotalTime = mPhaseShift * wobbleSpeed;
    }

    @Override
    public void frameUpdate(float deltaTime) {
        float sineDelta = -(float) Math.sin(mTotalTime);
        mTotalTime += deltaTime * wobbleSpeed;
        sineDelta += (float) Math.sin(mTotalTime);

        mTransform.translate(sineDelta * wobbleRange);
    }

    @Override
    public void onDestroy() {}
}
