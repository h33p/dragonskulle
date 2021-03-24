/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.dragonskulle.core.GameObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Base Transform class
 *
 * @author Harry Stoltz
 * @author Aurimas Blažulionis
 *     <p>All GameObjects will have a Transform object which allows it to be positioned, rotated and
 *     scaled in the world, and relative to parents The Transform can be used to get 3D position,
 *     scale and rotation.
 *     <p>More concrete transform types are used to modify the object's transformation.
 */
public abstract class Transform extends Component {
    protected static final float DEG_TO_RAD = (float) Math.PI / 180.f;

    protected final Matrix4f mInvMatrix = new Matrix4f();

    protected boolean mShouldUpdate = true;

    protected boolean mHasInverted = false;

    /**
     * Get the world matrix for this transform. If mShouldUpdate is true, it will then recursively
     * synchrinize the transformation.
     *
     * @return immutable World Matrix reference (can be changed by Transform between calls)
     */
    public abstract Matrix4fc getWorldMatrix();

    /**
     * Get the transformation matrix GameObject's children should use for inheriting
     * transformations. This might be different from the world matrix in places like UI
     *
     * @return immutable children transformation matrix reference, which can be changed by the
     *     transform.
     */
    public Matrix4fc getMatrixForChildren() {
        return getWorldMatrix();
    }

    public Matrix4fc getInvWorldMatrix() {
        if (mShouldUpdate || !mHasInverted) {
            mInvMatrix.set(getWorldMatrix()).invert();
            mHasInverted = true;
        }
        return mInvMatrix;
    }

    /** Set mShouldUpdate to true in all children transforms */
    protected void setUpdateFlag() {
        if (mShouldUpdate) {
            return;
        }
        mShouldUpdate = true;
        mHasInverted = false;
        for (GameObject obj : mGameObject.getChildren()) {
            obj.getTransform().setUpdateFlag();
        }
    }

    /**
     * Get the rotation of the transform in the world as a Quaternion
     *
     * @return Quaternionf containing the rotation of the transform
     */
    public Quaternionf getRotation() {
        Quaternionf rotation = new Quaternionf();
        getWorldMatrix().getNormalizedRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform in the world as a Quaternion
     *
     * @param dest Quaternionf to store the rotation of the transform
     */
    public void getRotation(Quaternionf dest) {
        getWorldMatrix().getNormalizedRotation(dest);
    }

    /**
     * Get the rotation of the transform in the world as axis angles
     *
     * @return AxisAngle4f containing the rotation of the transform
     */
    public AxisAngle4f getRotationAngles() {
        AxisAngle4f rotation = new AxisAngle4f();
        getWorldMatrix().getRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform in the world as axis angles
     *
     * @param dest AxisAnglef to store the rotation of the transform
     */
    public void getRotationAngles(AxisAngle4f dest) {
        getWorldMatrix().getRotation(dest);
    }

    /**
     * Get the position of the transform in the world
     *
     * @return Vector3f containing the world position
     */
    public Vector3f getPosition() {
        Vector3f position = new Vector3f();
        getWorldMatrix().getColumn(3, position);
        return position;
    }

    /**
     * Get the position of the transform in the world
     *
     * @param dest Vector3f to store the position
     */
    public void getPosition(Vector3f dest) {
        getWorldMatrix().getColumn(3, dest);
    }

    /**
     * Get the scale of the transform in the world
     *
     * @return Vector3f containing the scale of the transform
     */
    public Vector3f getScale() {
        Vector3f scale = new Vector3f();
        getWorldMatrix().getScale(scale);
        return scale;
    }

    /**
     * Get the scale of the transform in the world
     *
     * @param dest Vector3f to store the scale
     */
    public void getScale(Vector3f dest) {
        getWorldMatrix().getScale(dest);
    }

    /**
     * Get the right direction vector
     *
     * @return Vector3f containing the right direction
     */
    public Vector3f getRightVector() {
        Vector3f right = new Vector3f();
        getWorldMatrix().getColumn(0, right);
        return right;
    }

    /**
     * Get the right direction vector
     *
     * @param dest Vector3f to store the right direction
     */
    public void getRightVector(Vector3f dest) {
        getWorldMatrix().getColumn(0, dest);
    }

    /**
     * Get the up direction vector
     *
     * @return Vector3f containing the up direction
     */
    public Vector3f getUpVector() {
        Vector3f up = new Vector3f();
        getWorldMatrix().getColumn(1, up);
        return up;
    }

    /**
     * Get the up direction vector
     *
     * @param dest Vector3f to store the up direction
     */
    public void getUpVector(Vector3f dest) {
        getWorldMatrix().getColumn(1, dest);
    }

    /**
     * Get the forward direction vector
     *
     * @return Vector3f containing the forward direction
     */
    public Vector3f getForwardVector() {
        Vector3f forward = new Vector3f();
        getWorldMatrix().getColumn(1, forward);
        return forward;
    }

    /**
     * Get the forward direction vector
     *
     * @param dest Vector3f to store the forward direction
     */
    public void getForwardVector(Vector3f dest) {
        getWorldMatrix().getColumn(1, dest);
    }
}
