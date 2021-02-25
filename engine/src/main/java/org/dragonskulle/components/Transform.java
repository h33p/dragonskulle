/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import java.io.Serializable;
import java.util.ArrayList;

import org.dragonskulle.core.Reference;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Base Transform class
 *
 * @author Harry Stoltz
 *     <p>All GameObjects will have a Transform object which stores the position, rotation and scale
 *     of the object (As forward, right, up and position in a 4x4 Matrix). The Transform can be used
 *     to get 3D position, scale and rotation. Or can be cast to HexTransform to get the position,
 *     scale and rotation in Hex coordinates.
 */
public class Transform extends Component implements Serializable {

    private final Matrix4f mLocalMatrix;
    private Matrix4f mWorldMatrix;

    private boolean shouldUpdate = true;

    // TODO: Create new methods that take a Vector, Quaternion or matrix as an argument to store
    //      results in. Would save on allocations

    /** Default constructor. mLocalMatrix is just the identity matrix */
    public Transform() {
        mLocalMatrix = new Matrix4f().identity();
    }

    /**
     * Constructor
     *
     * @param position Starting position for the object
     */
    public Transform(Vector3f position) {
        mLocalMatrix = new Matrix4f().identity().translate(position);
    }

    /**
     * Constructor
     *
     * @param matrix Matrix to be used for mLocalMatrix
     */
    public Transform(Matrix4f matrix) {
        mLocalMatrix = new Matrix4f(matrix);
        setUpdateFlagInChildren();
    }

    /**
     * Set the transform position
     *
     * @param position New position for the transform
     */
    public void setPosition(Vector3f position) {
        mLocalMatrix.setColumn(3, new Vector4f(position, 1.f));
        setUpdateFlagInChildren();
    }

    /**
     * Rotate the object with euler angles
     *
     * @param eulerAngles Vector containing euler angles to rotate object with
     */
    public void rotate(Vector3f eulerAngles) {
        mLocalMatrix.rotateXYZ(eulerAngles);
        setUpdateFlagInChildren();
    }

    // TODO: Rotate with quaternion

    /**
     * Rotate the object with a quaternion
     *
     * @param quaternion Quaternion to rotate object with
     */
    public void rotate(Quaternionf quaternion) {
        mLocalMatrix.rotate(quaternion);
        setUpdateFlagInChildren();
    }

    /**
     * Translate the object
     *
     * @param translation Vector translation to perform
     */
    public void translate(Vector3f translation) {
        mLocalMatrix.translate(translation);
        setUpdateFlagInChildren();
    }

    /**
     * Scale the object
     *
     * @param scale Vector containing XYZ scale
     */
    public void scale(Vector3f scale) {
        mLocalMatrix.scale(scale);
        setUpdateFlagInChildren();
    }

    /**
     * Set mShouldUpdate to true in all children transforms
     */
    private void setUpdateFlagInChildren() {
        ArrayList<Reference<Transform>> childTransforms = new ArrayList<>();
        mGameObject.getComponentsInChildren(Transform.class, childTransforms);
        for (Reference<Transform> transformReference : childTransforms) {
            if (transformReference.isValid()) {
                Transform t = transformReference.get();
                t.shouldUpdate = true;
                t.setUpdateFlagInChildren();
            }
        }

    }

    /**
     * Get the transformation matrix relative to the parent transform
     *
     * @return A copy of the local matrix
     */
    public Matrix4f getLocalMatrix() {
        Matrix4f dest = new Matrix4f();
        mLocalMatrix.get(dest);
        return dest;
    }

    /**
     * Get the transformation matrix relative to the parent transform
     *
     * @param dest Matrix to store a copy of the local matrix
     */
    public void getLocalMatrix(Matrix4f dest) { dest.set(mLocalMatrix);}


    /**
     * Get the normalised rotation of the transform
     *
     * @return Rotation of the transform as Quaternion
     */
    public Quaternionf getLocalRotation() {
        Quaternionf rotation = new Quaternionf();
        mLocalMatrix.getNormalizedRotation(rotation);
        return rotation;
    }

    /**
     * Get the normalised rotation of the transform, relative to the parent transform
     *
     * @param dest Quaternion to store the rotation
     */
    public void getLocalRotation(Quaternionf dest) {
        mLocalMatrix.getNormalizedRotation(dest);
    }

    /**
     * Get the rotation of the transform per axis, in radians, relative to the parent transform
     *
     * @return Rotation of the transform as AxisAngle
     */
    public AxisAngle4f getLocalRotationAngles() {
        AxisAngle4f rotation = new AxisAngle4f();
        mLocalMatrix.getRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform per axis, in radians, relative to the parent transform
     *
     * @param dest AxisAngle4f to store the rotation
     */
    public void getLocalRotationAngles(AxisAngle4f dest) {
        mLocalMatrix.getRotation(dest);
    }

    /**
     * Get the position of the transform
     *
     * @return Vector3f containing the XYZ position of the object
     */
    public Vector3f getLocalPosition() {
        Vector3f position = new Vector3f();
        mLocalMatrix.getColumn(3, position);
        return position;
    }

    /**
     * Get the position of the transform relative to the parent transform
     *
     * @param dest
     */
    public void getLocalPosition(Vector3f dest) {
        mLocalMatrix.getColumn(3, dest);
    }

    /**
     * Get the scale of the transform
     *
     * @return Vector3f containing the XYZ scale of the object
     */
    public Vector3f getLocalScale() {
        Vector3f scale = new Vector3f();
        mLocalMatrix.getScale(scale);
        return scale;
    }

    /**
     * Get the local forward vector
     *
     * @return New Vector3f containing the forward vector
     */
    public Vector3f getLocalForward() {
        Vector3f forward = new Vector3f();
        mLocalMatrix.getColumn(0, forward);
        return forward;
    }

    /**
     * Get the local right vector
     *
     * @return New Vector3f containing the right vector
     */
    public Vector3f getLocalRight() {
        Vector3f right = new Vector3f();
        mLocalMatrix.getColumn(1, right);
        return right;
    }

    /**
     * Get the local up vector
     *
     * @return New Vector3f containing the up vector
     */
    public Vector3f getLocalUp() {
        Vector3f up = new Vector3f();
        mLocalMatrix.getColumn(2, up);
        return up;
    }

    /**
     * Get the world matrix for this transform. If the transform is on a root object, mLocalMatrix
     * is used as the worldMatrix. If it isn't a root object and mShouldUpdate is true, recursively
     * call getWorldMatrix up to the first Transform that has mShouldUpdate as false
     *
     * @return mWorldMatrix
     */
    public Matrix4f getWorldMatrix() {
        if (shouldUpdate) {
            shouldUpdate = false;
            if (mGameObject.isRootObject()) {
                mWorldMatrix = mLocalMatrix;
            } else {
                mWorldMatrix = mGameObject.getParentTransform().getWorldMatrix().mul(mLocalMatrix);
            }
        }
        return mWorldMatrix;
    }

    /**
     * Get the rotation of the transform in the world as a Quaternion
     *
     * @return New Quaternionf containing the rotation of the transform
     */
    public Quaternionf getRotation() {
        Quaternionf rotation = new Quaternionf();
        getWorldMatrix().getNormalizedRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform in the world as axis angles
     *
     * @return New AxisAngle4f containing the r
     */
    public AxisAngle4f getRotationAngles() {
        AxisAngle4f rotation = new AxisAngle4f();
        getWorldMatrix().getRotation(rotation);
        return rotation;
    }

    public Vector3f getPosition() {
        Vector3f position = new Vector3f();
        getWorldMatrix().getColumn(3, position);
        return position;
    }

    public Vector3f getScale() {
        Vector3f scale = new Vector3f();
        getWorldMatrix().getScale(scale);
        return scale;
    }

    @Override
    protected void onDestroy() {

        // TODO: Destroy for transform

    }
}
