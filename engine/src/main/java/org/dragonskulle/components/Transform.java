/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import java.io.Serializable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Base Transform class
 *
 * @author Harry Stoltz
 *     <p>All GameObjects will have a Transform object which stores the position, rotation and scale
 *     of the object (As right, up, forward and position in a 4x4 Matrix). The Transform can be used
 *     to get 3D position, scale and rotation. Or can be cast to HexTransform to get the position,
 *     scale and rotation in Hex coordinates.
 */
public class Transform extends Component implements Serializable {

    Matrix4f mLocalMatrix;
    Matrix4f mWorldMatrix;

    // TODO: Should an update to this transform be propagated to all child transforms?
    //      Or should the world matrix only be updated

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
    }

    /**
     * Set the transform position
     *
     * @param position New position for the transform
     */
    public void setPosition(Vector3f position) {
        mLocalMatrix.setColumn(3, new Vector4f(position, 1.f));
    }

    /**
     * Rotate the object with euler angles
     *
     * @param eulerAngles Vector containing euler angles to rotate object by
     */
    public void rotate(Vector3f eulerAngles) {
        mLocalMatrix.rotateXYZ(eulerAngles);
    }

    // TODO: Rotate with quaternion

    /**
     * Translate the object
     *
     * @param translation Vector translation to perform
     */
    public void translate(Vector3f translation) {
        mLocalMatrix.translate(translation);
    }

    /**
     * Scale the object
     *
     * @param scale Vector containing XYZ scale
     */
    public void scale(Vector3f scale) {
        mLocalMatrix.scale(scale);
    }

    /**
     * Getter for mLocalMatrix
     *
     * @return mLocalMatrix
     */
    public Matrix4f getLocalMatrix() {
        return mLocalMatrix;
    }

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
     * Get the scale of the transform
     *
     * @return Vector3f containing the XYZ scale of the object
     */
    public Vector3f getLocalScale() {
        Vector3f scale = new Vector3f();
        mLocalMatrix.getScale(scale);
        return scale;
    }

    // TODO: Getters for forward, right and up?

    /**
     * Getter for mWorldMatrix
     *
     * @return mWorldMatrix
     */
    public Matrix4f getWorldMatrix() {
        return mWorldMatrix;
    }

    // TODO: Getters for world position, rotation and scale

    public Quaternionf getRotation() {
        return null;
    }

    public Vector3f getPosition() {
        return null;
    }

    public Vector3f getScale() {
        return null;
    }

    @Override
    protected void onDestroy() {

        // TODO: Destroy for transform

    }
}
