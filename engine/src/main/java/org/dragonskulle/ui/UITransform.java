/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import static org.joml.Math.min;

import lombok.Getter;
import lombok.Setter;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Transform;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * UI Transform class
 *
 * @author Aurimas Blažulionis
 *     <p>This transform class overrides several {@link Transform} methods to provide screen aspect
 *     ratio correction.
 */
public class UITransform extends Transform {
    /** Maintain aspect ratio of the UI element */
    @Getter @Setter private boolean mMaintainAspect;

    private final Matrix4f mTmpScaledLocalMatrix = new Matrix4f();
    private final Vector3f mTmpParentScaleVec = new Vector3f();
    private float mParentAspect = 0.f;

    public UITransform(boolean maintainAspect) {
        super();
        mMaintainAspect = maintainAspect;
    }

    public UITransform() {
        this(false);
    }

    public UITransform(float width, float height, boolean scaleToScreen) {
        this(scaleToScreen);
        mLocalMatrix.scaleLocal(width, height, 1f);
    }

    public UITransform(float width, float height) {
        this(width, height, false);
    }

    /**
     * Get the world matrix for this transform. If the transform is on a root object, mLocalMatrix
     * is used as the worldMatrix. If it isn't a root object and mShouldUpdate is true, recursively
     * call getWorldMatrix up to the first Transform that has mShouldUpdate as false
     *
     * @return constant mWorldMatrix
     */
    @Override
    public Matrix4fc getWorldMatrix() {

        boolean isRoot = getGameObject().isRootObject();

        if (mMaintainAspect) {
            float newParentAspect = 1.f;

            Camera mainCam = Camera.getMainCamera();
            if (mainCam != null) {
                newParentAspect = mainCam.getAspectRatio();
            }

            if (!isRoot) {
                mGameObject.getParentTransform().getWorldMatrix().getScale(mTmpParentScaleVec);
                newParentAspect *= mTmpParentScaleVec.x() / mTmpParentScaleVec.y();
            }

            mShouldUpdate = mShouldUpdate || mParentAspect == newParentAspect;
            mParentAspect = newParentAspect;
        }

        if (mShouldUpdate) {
            mShouldUpdate = false;
            mTmpScaledLocalMatrix.set(mLocalMatrix);

            // Here we need to scale ourselves to correct screen aspect ratio
            if (mMaintainAspect) {
                mTmpScaledLocalMatrix.scale(
                        min(1.f / mParentAspect, 1.f), min(mParentAspect, 1.f), 1f);
            }

            if (isRoot) {
                mWorldMatrix.set(mTmpScaledLocalMatrix);
            } else {
                // Multiply parent's world matrix by the local matrix
                // Which gives us the matrix multiplication mLocalMatrix * parentWorldMatrix
                // so when doing mWorldMatrix * (vector)
                // It is the same as doing parentWorldMatrix * mLocalMatrix * (vector)
                // so that any parent transformations are done prior to the local transformation
                mGameObject
                        .getParentTransform()
                        .getWorldMatrix()
                        .mul(mTmpScaledLocalMatrix, mWorldMatrix);
            }
        }

        return mWorldMatrix;
    }
}
