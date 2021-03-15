/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Transform;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * UI Transform class
 *
 * @author Aurimas Blažulionis
 *     <p>This transform class overrides several {@link Transform} methods to provide screen aspect
 *     ratio correction.
 */
@Accessors(prefix = "m")
public class UITransform extends Transform {
    /** Maintain aspect ratio of the UI element */
    @Getter @Setter private boolean mMaintainAspect;
    /** Target aspect ratio of the element */
    @Getter @Setter private float mTargetAspectRatio = 1.f;

    /** Describes how local coordinates are anchored to parent */
    private final Vector4f mParentAnchor = new Vector4f(0f, 0f, 1f, 1f);

    /** Additional bounds extending/subtracting from parent anchor */
    private final Vector4f mMargin = new Vector4f(0f);

    /** Base position of the transform */
    private final Vector2f mPosition = new Vector2f(0f);
    /** Controls which part of the rect is on position. I.e. controls center of rotation */
    private final Vector2f mPivotOffset = new Vector2f(0.5f);
    /** Scale of the transform, propagated to child transforms */
    private final Vector2f mScale = new Vector2f(1f);

    private final Vector4f mLocalCorners = new Vector4f(0f);
    private final Vector4f mScaledLocalCorners = new Vector4f(0f);

    // @Getter
    private float mRotation = 0f;

    /** Whether or not we should clip children that are out of bounds */
    // TODO: Actually somehow implement this
    @Getter @Setter private boolean mClipChildren = false;

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

    private void updateLocalCorners() {
        // Update local corners
        mLocalCorners.set(mParentAnchor);
        mLocalCorners.add(mMargin);

        Transform parentGen = getGameObject().getParentTransform();

        Vector4f off = new Vector4f(0f);

        // If we have a proper UI transform get the parent scaled corners
        if (parentGen != null && parentGen instanceof UITransform) {
            UITransform parent = (UITransform) parentGen;
            mScaledLocalCorners.set(parent.getScaledLocalCorners());
        } else {
            // Otherwise just use regular screen bounds
            Camera main = Camera.getMainCamera();
            float width = main == null ? 1f : main.getAspectRatio();
            mScaledLocalCorners.set(-width, -1f, width, 1f);
            off.set(-width, -1f, -width, -1f);
        }

        float width = mScaledLocalCorners.z() - mScaledLocalCorners.x();
        float height = mScaledLocalCorners.w() - mScaledLocalCorners.y();

        mScaledLocalCorners.set(width, height, width, height);
        mScaledLocalCorners.mul(mLocalCorners);

        mScaledLocalCorners.add(off);

        // Right here we have a chance to fit object's aspect ratio
        if (mMaintainAspect) {
            float curWidth = mScaledLocalCorners.z() - mScaledLocalCorners.x();
            float curHeight = mScaledLocalCorners.w() - mScaledLocalCorners.y();

            float targetHeight = curWidth / mTargetAspectRatio;
            float targetWidth = curHeight * mTargetAspectRatio;

            if (targetHeight < curHeight) {
                float heightDiff = curHeight - targetHeight;
                float halfHeight = heightDiff * 0.5f;
                mScaledLocalCorners.add(0f, halfHeight, 0f, -halfHeight);
            } else {
                float widthDiff = curWidth - targetWidth;
                float halfWidth = widthDiff * 0.5f;
                mScaledLocalCorners.add(halfWidth, 0f, -halfWidth, 0f);
            }
        }
    }

    private Matrix4f mCornerMatrix = new Matrix4f();
    private Matrix4f mLocalCornersMatrix = new Matrix4f();
    private Matrix4f mScreenCornerMatrix = new Matrix4f();

    public Matrix4fc cornersToWorld() {

        mCornerMatrix.identity().set(getWorldMatrix());

        float scaleX = mScaledLocalCorners.z() - mScaledLocalCorners.x();
        float scaleY = mScaledLocalCorners.w() - mScaledLocalCorners.y();

        float pivotX = mPivotOffset.x() * scaleX;
        float pivotY = mPivotOffset.y() * scaleY;

        // TODO: fix scaling issues

        // We want to apply rotation on object's pivot point
        mLocalCornersMatrix
                .identity()
                .scale(scaleX, scaleY, 1f)
                .translateLocal(-pivotX, -pivotY, 0f);

        // We multiply by parent and shift it back to where it belongs
        mCornerMatrix.mul(mLocalCornersMatrix);

        mCornerMatrix.translateLocal(pivotX, pivotY, 0f);

        return mCornerMatrix;
    }

    public Matrix4fc cornersToScreen() {
        mScreenCornerMatrix.set(cornersToWorld());

        mScreenCornerMatrix.scaleLocal(1.f / mScreenAspectRatio, 1f, 1f);

        return mScreenCornerMatrix;
    }

    private Matrix4f mBoxMatrix = new Matrix4f();

    private float mScreenAspectRatio = 1f;

    /** Check whether screen aspect ratio changed. If so, dirty all transforms */
    private void checkScrenChange() {
        Camera main = Camera.getMainCamera();
        float width = main == null ? 1f : main.getAspectRatio();

        if (mScreenAspectRatio != width) setUpdateFlag();

        mScreenAspectRatio = width;
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

        checkScrenChange();

        if (mShouldUpdate) {
            mShouldUpdate = false;
            updateLocalCorners();

            mBoxMatrix
                    .identity()
                    .translate(mPosition.x(), mPosition.y(), 0f)
                    .translate(mScaledLocalCorners.x(), mScaledLocalCorners.y(), 0f)
                    .rotateZ(mRotation)
                    .scaleLocal(mScale.x(), mScale.y(), 1f);

            mWorldMatrix.set(mBoxMatrix);

            Transform parentTransform = getGameObject().getParentTransform();

            if (parentTransform != null) {
                parentTransform.getWorldMatrix().mul(mBoxMatrix, mWorldMatrix);
            }
        }

        return mWorldMatrix;
    }

    public Vector4fc getLocalCorners() {
        if (mShouldUpdate) updateLocalCorners();
        return mLocalCorners;
    }

    public Vector4fc getScaledLocalCorners() {
        if (mShouldUpdate) updateLocalCorners();
        return mScaledLocalCorners;
    }

    // TODO: fix scaling issues and expose this
    /*private void scale(float scale) {
        mScale.mul(scale);
        setUpdateFlag();
    }

    private void scale(float x, float y) {
        mScale.mul(x, y);
        setUpdateFlag();
    }*/

    /** Set parent anchor position */
    public void setParentAnchor(float offset) {
        mParentAnchor.set(offset, offset, 1f - offset, 1f - offset);
        setUpdateFlag();
    }

    public void setParentAnchor(float x, float y) {
        mParentAnchor.set(x, y, 1f - x, 1f - y);
        setUpdateFlag();
    }

    public void setParentAnchor(float x, float y, float z, float w) {
        mParentAnchor.set(x, y, z, w);
        setUpdateFlag();
    }

    public void setMargin(float margin) {
        mMargin.set(margin, margin, -margin, -margin);
        setUpdateFlag();
    }

    public void setMargin(float x, float y) {
        mMargin.mul(x, y, -x, -y);
        setUpdateFlag();
    }

    public void setMargin(float x, float y, float z, float w) {
        mMargin.set(x, y, z, w);
        setUpdateFlag();
    }

    public void rotateDeg(float deg) {
        mRotation += deg * Transform.DEG_TO_RAD;
        setUpdateFlag();
    }

    public void setPosition(float x, float y) {
        mPosition.set(x, y);
        setUpdateFlag();
    }

    public void translate(float x, float y) {
        mPosition.add(x, y);
        setUpdateFlag();
    }
}
