/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform;

/**
 * Simple wobbly objects!
 *
 * @author Aurimas Blažulionis
 */
public class Wobbler extends Component implements IOnAwake, IFrameUpdate {
    public float wobbleSpeed = 10.f;
    public float wobbleRange = 5.f;

    private Transform mTransform;
    private float mTotalTime = 0.f;

    public Wobbler() {}

    public Wobbler(float wobbleSpeed, float wobbleRange) {
        this.wobbleSpeed = wobbleSpeed;
        this.wobbleRange = wobbleRange;
    }

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform();
    }

    @Override
    public void frameUpdate(float deltaTime) {
        float sineDelta = -(float) Math.sin(mTotalTime);
        mTotalTime += deltaTime * wobbleSpeed;
        sineDelta += (float) Math.sin(mTotalTime);

        mTransform.rotateDeg(sineDelta * wobbleRange, 0.f, 0.f);
    }

    @Override
    public void onDestroy() {}
}
