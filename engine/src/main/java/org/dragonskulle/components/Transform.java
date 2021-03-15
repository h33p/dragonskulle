/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.dragonskulle.core.GameObject;
import org.joml.AxisAngle4f;
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

    protected boolean mShouldUpdate = true;

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

    /** Set mShouldUpdate to true in all children transforms */
    protected void setUpdateFlag() {
        if (mShouldUpdate) {
            return;
        }
        mShouldUpdate = true;
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
}
