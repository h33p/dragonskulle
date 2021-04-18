/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Transform;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.utils.MathUtils;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
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
public class TransformUI extends Transform {
    /** Maintain aspect ratio of the UI element */
    @Getter @Setter private boolean mMaintainAspect;
    /** Target aspect ratio of the element */
    @Getter private float mTargetAspectRatio = 1.f;

    /** Describes how local coordinates are anchored to parent */
    private final Vector4f mParentAnchor = new Vector4f(0f, 0f, 1f, 1f);

    /** Additional bounds extending/subtracting from parent anchor */
    private final Vector4f mMargin = new Vector4f(0f);

    /** Base position of the transform */
    private final Vector2f mPosition = new Vector2f(0f);
    /**
     * Controls which part of the rect is on position. I.e. controls center of rotation
     *
     * <p>These coordinates are in range [[0; 1], [0; 1]]
     */
    private final Vector2f mPivotOffset = new Vector2f(0.5f);
    /** Scale of the transform, propagated to child transforms */
    private final Vector2f mScale = new Vector2f(1f);

    private final Vector4f mLocalCorners = new Vector4f(0f);
    private final Vector4f mScaledLocalCorners = new Vector4f(0f);

    private final Vector4f mParentCorners = new Vector4f(0f);

    private float mRotation = 0f;

    /** Whether or not we should clip children that are out of bounds */
    // TODO: Actually somehow implement this
    @Getter @Setter private boolean mClipChildren = false;

    public TransformUI(boolean maintainAspect) {
        super();
        mMaintainAspect = maintainAspect;
    }

    public TransformUI() {
        this(false);
    }

    public TransformUI(float width, float height, boolean scaleToScreen) {
        this(scaleToScreen);
    }

    public TransformUI(float width, float height) {
        this(width, height, false);
    }

    private final Vector2f mParentPivotPoint = new Vector2f();

    private void updateParentCorners() {
        Transform parentGen = getGameObject().getParentTransform();

        // If we have a proper UI transform get the parent scaled corners
        if (parentGen != null && parentGen instanceof TransformUI) {
            TransformUI parent = (TransformUI) parentGen;
            mParentCorners.set(parent.getScaledLocalCorners());
            mParentPivotPoint.set(parent.mPivotOffset);
        } else {
            // Otherwise just use regular screen bounds
            Camera main = Scene.getActiveScene().getSingleton(Camera.class);
            float width = main == null ? 1f : main.getAspectRatio();
            mParentCorners.set(-width, -1f, width, 1f);
            mParentPivotPoint.set(0.5f, 0.5f);
        }

        float width = mParentCorners.z() - mParentCorners.x();
        float height = mParentCorners.w() - mParentCorners.y();

        mParentPivotPoint.mul(width, height);
    }

    private void updateLocalCorners() {
        // Update local corners
        mLocalCorners.set(mParentAnchor);
        mLocalCorners.add(mMargin);

        updateParentCorners();
        mScaledLocalCorners.set(mParentCorners);

        float width = mScaledLocalCorners.z() - mScaledLocalCorners.x();
        float height = mScaledLocalCorners.w() - mScaledLocalCorners.y();

        mScaledLocalCorners.set(width, height, width, height);
        mScaledLocalCorners.mul(mLocalCorners);

        float ox = mScaledLocalCorners.x();
        float oy = mScaledLocalCorners.y();

        width = mScaledLocalCorners.z() - mScaledLocalCorners.x();
        height = mScaledLocalCorners.w() - mScaledLocalCorners.y();

        float pivotX = mPivotOffset.x() * width + ox;
        float pivotY = mPivotOffset.y() * height + oy;

        mScaledLocalCorners.sub(pivotX, pivotY, pivotX, pivotY);

        // Right here we have a chance to fit object's aspect ratio
        if (mMaintainAspect) {
            float curWidth = mScaledLocalCorners.z() - mScaledLocalCorners.x();
            float curHeight = mScaledLocalCorners.w() - mScaledLocalCorners.y();

            float targetHeight = curWidth / mTargetAspectRatio;
            float targetWidth = curHeight * mTargetAspectRatio;

            if (targetHeight < curHeight) {
                float factor = targetHeight / curHeight;
                mScaledLocalCorners.mul(1, factor, 1, factor);
            } else {
                float factor = targetWidth / curWidth;
                mScaledLocalCorners.mul(factor, 1, factor, 1);
            }
        }

        mScaledLocalCorners.add(pivotX, pivotY, pivotX, pivotY);

        mPivotPoint.set(
                MathUtils.lerp(mScaledLocalCorners.x(), mScaledLocalCorners.z(), mPivotOffset.x()),
                MathUtils.lerp(mScaledLocalCorners.y(), mScaledLocalCorners.w(), mPivotOffset.y()));
    }

    private Matrix4f mCornerMatrix = new Matrix4f();
    private Matrix4f mLocalCornersMatrix = new Matrix4f();
    private Matrix4f mScreenCornerMatrix = new Matrix4f();

    private final Vector2f mPivotPoint = new Vector2f();

    public Matrix4fc cornersToWorld() {
        mCornerMatrix.identity().set(getMatrixForChildren());

        float scaleX = mScaledLocalCorners.z() - mScaledLocalCorners.x();
        float scaleY = mScaledLocalCorners.w() - mScaledLocalCorners.y();

        // We want to apply rotation on object's pivot point. Shift the rectangle
        // to counteract the shift that happens in getMatrixForChildren
        mLocalCornersMatrix
                .identity()
                .translate(-mPivotPoint.x(), -mPivotPoint.y(), 0)
                .translate(mScaledLocalCorners.x(), mScaledLocalCorners.y(), 0)
                .scale(scaleX, scaleY, 1f);

        // We multiply by parent and shift it back to where it belongs
        mCornerMatrix.mul(mLocalCornersMatrix);

        return mCornerMatrix;
    }

    private Matrix4f mBoxMatrix = new Matrix4f();

    private float mScreenAspectRatio = 1f;

    /** Check whether screen aspect ratio changed. If so, dirty all transforms */
    private void checkScreenChange() {
        Camera main = Scene.getActiveScene().getSingleton(Camera.class);
        float width = main == null ? 1f : main.getAspectRatio();

        if (mScreenAspectRatio != width) setUpdateFlag();

        mScreenAspectRatio = width;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public Matrix4fc getWorldMatrix() {
        mScreenCornerMatrix.set(cornersToWorld());

        mScreenCornerMatrix.scaleLocal(1.f / mScreenAspectRatio, 1f, 1f);

        return mScreenCornerMatrix;
    }

    /**
     * Get the world matrix for this transform. If the transform is on a root object, mLocalMatrix
     * is used as the worldMatrix. If it isn't a root object and mShouldUpdate is true, recursively
     * call getWorldMatrix up to the first Transform that has mShouldUpdate as false
     *
     * @return constant mWorldMatrix
     */
    @Override
    public Matrix4fc getMatrixForChildren() {
        checkScreenChange();

        if (mShouldUpdate) {
            updateLocalCorners();
            mShouldUpdate = false;

            float tx = mPivotPoint.x() - mParentPivotPoint.x();
            float ty = mPivotPoint.y() - mParentPivotPoint.y();

            mBoxMatrix
                    .identity()
                    .translate(mPosition.x(), mPosition.y(), 0f)
                    .translate(tx, ty, 0)
                    .rotateZ(mRotation)
                    .scale(mScale.x(), mScale.y(), 1f);

            Transform parentTransform = getGameObject().getParentTransform();

            Matrix4fc parentMatrix =
                    parentTransform != null ? parentTransform.getMatrixForChildren() : null;

            if (parentMatrix != null) {
                parentMatrix.mul(mBoxMatrix, mBoxMatrix);
            }
        }

        return mBoxMatrix;
    }

    public void setTargetAspectRatio(float targetAspectRatio) {
        mTargetAspectRatio = targetAspectRatio;
        setUpdateFlag();
    }

    /**
     * Sets the local 3D transformation
     *
     * <p>This method sets the local transformation of the object to roughly match the input data.
     *
     * <p>Only 2D positioning is going to take place
     *
     * <p>Rotation will be projected and only Z axis is going to be preserved.
     *
     * <p>Only 2D scaling is going to be preserved
     *
     * @param position target local position to set
     * @param rotation target local rotation to set
     * @param scale target local scale to set
     */
    @Override
    public void setLocal3DTransformation(
            Vector3fc position, Quaternionfc rotation, Vector3fc scale) {
        mPosition.set(position.x(), position.y());
        AxisAngle4f rotAxis = rotation.get(new AxisAngle4f());
        mRotation = rotAxis.z * rotAxis.angle;
        mScale.set(scale.x(), scale.y());
        setUpdateFlag();
    }

    public Vector4fc getLocalCorners() {
        if (mShouldUpdate) updateLocalCorners();
        return mLocalCorners;
    }

    public Vector4fc getScaledLocalCorners() {
        if (mShouldUpdate) updateLocalCorners();
        return mScaledLocalCorners;
    }

    public void scale(float scale) {
        mScale.mul(scale);
        setUpdateFlag();
    }

    public void scale(float x, float y) {
        mScale.mul(x, y);
        setUpdateFlag();
    }

    public void setScale(float scale) {
        mScale.set(scale);
        setUpdateFlag();
    }

    public void setScale(float x, float y) {
        mScale.set(x, y);
        setUpdateFlag();
    }

    public Vector2fc getPivotOffset() {
        return mPivotOffset;
    }

    /** Set the pivot offset */
    public void setPivotOffset(float x, float y) {
        mPivotOffset.set(x, y);
        setUpdateFlag();
    }

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
        mMargin.set(margin, -margin, -margin, margin);
        setUpdateFlag();
    }

    public void setMargin(float x, float y) {
        mMargin.set(x, y, -x, -y);
        setUpdateFlag();
    }

    public void setMargin(float x, float y, float z, float w) {
        mMargin.set(x, y, z, w);
        setUpdateFlag();
    }

    public void rotateDeg(float deg) {
        mRotation += deg * MathUtils.DEG_TO_RAD;
        setUpdateFlag();
    }

    public void setRotationDeg(float deg) {
        mRotation = deg * MathUtils.DEG_TO_RAD;
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
