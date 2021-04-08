/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector3f;

/**
 * Allows to move an object towards a target
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class TargetMovement extends Component implements IFrameUpdate, IOnAwake {
    @Getter @Setter public float mMinMoveSpeed = 10f;
    @Getter @Setter public float mMaxMoveSpeed = 90f;

    @Getter @Setter public float mMaxSpeedDistance = 10f;

    @Getter @Setter private float mSpeed = 10f;
    @Getter @Setter private float mEndDelta = 0.1f;

    @Getter @Setter private float mHoldTime = 0.1f;

    @Getter private Vector3f mDirectionMul = new Vector3f(1f, 1f, 0f);

    private transient Transform3D mTransform;

    private Vector3f mTmpVec1 = new Vector3f();
    private Vector3f mTmpVec2 = new Vector3f();
    private float curHoldTime = 0f;

    @Getter private Reference<Transform> mTarget = null;

    public void setTarget(Transform target) {
        mTarget = target.getReference(Transform.class);
    }

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
        Scene.getActiveScene().registerSingleton(this);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mTransform != null && mTarget != null && mTarget.isValid()) {

            mTransform.getPosition(mTmpVec1);
            mTarget.get().getPosition(mTmpVec2);
            System.out.println(String.format("%.2f %.2f %.2f", mTmpVec2.x, mTmpVec2.y, mTmpVec2.z));
            mTmpVec2.sub(mTmpVec1).mul(mDirectionMul);
            System.out.println(String.format("%.2f %.2f %.2f", mTmpVec2.x, mTmpVec2.y, mTmpVec2.z));

            if (mTmpVec2.lengthSquared() <= mEndDelta * mEndDelta) {
                if (curHoldTime >= mHoldTime) {
                    System.out.println("BREAK OUT OF HOLD!!!");
                    mTarget = null;
                    return;
                }
            } else {
                curHoldTime = 0f;
            }

            curHoldTime += deltaTime;

            float dist = mTmpVec2.length();

            if (dist <= 1e-20f) return;

            float moveSpeed =
                    MathUtils.lerp(
                            mMinMoveSpeed,
                            mMaxMoveSpeed,
                            Math.min(mMaxSpeedDistance, dist) / mMaxSpeedDistance);

            System.out.println(String.format("%.2f | %.2f", dist, moveSpeed * deltaTime));
            System.out.println(String.format("%.2f %.2f %.2f", mTmpVec2.x, mTmpVec2.y, mTmpVec2.z));

            mTmpVec2.normalize().mul(Math.min(moveSpeed * deltaTime, dist));

            mTmpVec2.mulDirection(mTransform.getInvWorldMatrix());

            mTransform.translate(mTmpVec2);
        }
    }

    @Override
    protected void onDestroy() {}
}
