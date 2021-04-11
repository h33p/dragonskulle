/* (C) 2021 DragonSkulle */

package org.dragonskulle.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Represents an objects position in hex coordinates.
 *
 * @author Harry Stoltz
 *
 *     <p>This extends Transform, and provides transformations to be done in hex (axial)
 *     coordinates.
 */
@Accessors(prefix = "m")
public class TransformHex extends Transform {
    public static final float HEX_SIZE = 1f;
    public static final float HEX_WIDTH = (float) Math.sqrt(3) * HEX_SIZE;
    public static final float HEX_HEIGHT = 2 * HEX_SIZE;

    // Matrix that takes a cartesian coordinate into hex coordinate space
    // It's a 3x3 matrix so that a 3d vector can still be multiplied
    public static final Matrix3f WORLD_TO_HEX =
            new Matrix3f((float) Math.sqrt(3) / 3, 0, 0, -1f / 3f, 2f / 3f, 0, 0, 0, 0);

    public static final Matrix3f HEX_TO_WORLD =
            new Matrix3f((float) Math.sqrt(3), 0, 0, (float) Math.sqrt(3) / 2, 3f / 2f, 0, 0, 0, 0);

    private Vector3f mPosition = new Vector3f();

    private float mRotation = 0f;
    @Getter private float mHeight = 0f;

    private Vector3f mCartesianPosition = new Vector3f();
    private Matrix4f mWorldMatrix = new Matrix4f();
    private Matrix4f mLocalMatrix = new Matrix4f();

    /** Default constructor for TransformHex. */
    public TransformHex() {}

    /**
     * Create a new Transform from Hex coordinates. This can be used for instantiating GameObjects
     * at a given Hex position.
     *
     * @param axial The axial coordinates for the transform
     */
    public TransformHex(Vector3fc axial) {
        setPosition(axial);
    }

    public TransformHex(float q, float r, float height) {
        setPosition(q, r);
        mHeight = height;
    }

    public TransformHex(float q, float r) {
        setPosition(q, r);
    }

    /**
     * Translate the object locally.
     *
     * @param axial axial coordinates to translate the object with on the transformation plane
     */
    public void translate(Vector3fc axial) {
        translate(axial, 0f);
    }

    /**
     * Translate the object locally.
     *
     * @param axial axial coordinates to translate the object with on the transformation plane
     * @param height vertical transformation
     */
    public void translate(Vector3fc axial, float height) {
        translate(axial.x(), axial.y(), height);
    }

    /**
     * Translate the object locally.
     *
     * @param q q axial coordinate
     * @param r r axial coordinate
     */
    public void translate(float q, float r) {
        mPosition.add(q, r, 0f);
        setUpdateFlag();
    }

    /**
     * Translate the object locally.
     *
     * @param q q axial coordinate
     * @param r r axial coordinate
     * @param height vertical height to translate
     */
    public void translate(float q, float r, float height) {
        mPosition.add(q, r, 0f);
        mHeight += height;
        setUpdateFlag();
    }

    /**
     * Translate the object locally.
     *
     * @param height vertical height to translate
     */
    public void translate(float height) {
        mHeight += height;
        setUpdateFlag();
    }

    /**
     * Set the object's height.
     *
     * @param height new height
     */
    public void setHeight(float height) {
        mHeight = height;
        setUpdateFlag();
    }

    /**
     * Set the position of the transform to the provided hex coordinates. The cartesian position
     * will be the centre point of the hex.
     *
     * @param axial Coordinates of the position in axial coordinate system
     */
    public void setPosition(Vector3fc axial) {
        mPosition.set(axial);
        setUpdateFlag();
    }

    /**
     * Set the position of the transform to the provided hex coordinates. The cartesian position
     * will be the centre point of the hex.
     *
     * @param q q coordinate in axial coordinate system
     * @param r r coordinate in axial coordinate system
     * @param s s coordinate in axial coordinate system. Must be (-q -r) to make sense.
     */
    public void setPosition(float q, float r, float s) {
        mPosition.set(q, r, s);
        setUpdateFlag();
    }

    public void setPosition(float q, float r) {
        mPosition.set(q, r, -q - r);
        setUpdateFlag();
    }

    /**
     * Get the position of a hex in axial coordinates. The z component of the vector is always 0.
     *
     * @param dest matrix to write the position to
     * @return Vector3f containing the axial coordinates (dest)
     */
    public Vector3f getLocalPosition(Vector3f dest) {
        dest.set(mPosition);
        return dest;
    }

    /**
     * Get the position of a hex in axial coordinates, rounded to nearest integer coordinate. The z
     * component of the vector is always 0.
     *
     * @param dest matrix to write the position to
     * @return Vector3f containing the axial coordinates (dest)
     */
    public Vector3f getRoundedLocalPosition(Vector3f dest) {
        dest.set(mPosition);
        return TransformHex.roundAxial(dest);
    }

    /** Gets the world transformation matrix. */
    @Override
    public Matrix4fc getWorldMatrix() {
        if (mShouldUpdate) {
            mShouldUpdate = false;

            mCartesianPosition.set(mPosition);
            axialToCartesian(mCartesianPosition, mHeight);

            mLocalMatrix.identity().translate(mCartesianPosition).rotateZ(mRotation);

            Transform parent = mGameObject.getParentTransform();
            if (parent != null) mWorldMatrix.set(parent.getMatrixForChildren());
            else mWorldMatrix.identity();

            mWorldMatrix.mul(mLocalMatrix);
        }

        return mWorldMatrix;
    }

    /**
     * Sets the local 3D transformation.
     *
     * <p>This method sets the local transformation of the object to roughly match the input data.
     *
     * <p>Rotation will be projected and only Z axis is going to be preserved.
     *
     * <p>Scaling is going to be fully ignored.
     *
     * @param position target local position to set
     * @param rotation target local rotation to set
     * @param scale target local scale to set
     */
    @Override
    public void setLocal3DTransformation(
            Vector3fc position, Quaternionfc rotation, Vector3fc scale) {
        mPosition.set(position);
        cartesianToAxial(mPosition);
        mHeight = position.z();
        AxisAngle4f rotAxis = rotation.get(new AxisAngle4f());
        mRotation = rotAxis.z * rotAxis.angle;
        setUpdateFlag();
    }

    @Override
    public void onDestroy() {}

    /**
     * Convert a vector containing axial coordinates to their equivalent cartesian coordinates. The
     * conversion is done in place, so make sure that you no longer need the vector containing the
     * axial coordinates.
     *
     * @param axial Vector3f with axial coordinates to convert
     */
    public static void axialToCartesian(Vector3f axial, float height) {
        // Multiply axial by the HEX_TO_PIXEL matrix
        axial.mul(HEX_TO_WORLD);

        // And then multiply q and r by HEX_SIZE
        axial.x *= HEX_SIZE;
        axial.y *= HEX_SIZE;
        axial.z = height;
    }

    /**
     * Get the nearest hex from a fractional hex position.
     *
     * <p>Algorithm from: https://www.redblobgames.com/grids/hexagons/#rounding
     *
     * @param axialPoint The fractional axial coordinate
     * @return Vector3f containing the actual hex coordinates
     */
    public static Vector3f roundAxial(Vector3f axialPoint) {
        // First convert the axial coordinates to cube:
        Vector3f rounded = TransformHex.axialToCube(axialPoint);

        // Round x, y and z
        float rx = Math.round(rounded.x);
        float ry = Math.round(rounded.y);
        float rz = Math.round(rounded.z);

        // Then reset the component with the largest difference after rounding so that x + y + z = 0

        float deltaX = Math.abs(rx - rounded.x);
        float deltaY = Math.abs(ry - rounded.y);
        float deltaZ = Math.abs(rz - rounded.z);

        if (deltaX > deltaY && deltaX > deltaZ) {
            rx = -ry - rz;
        } else if (deltaY > deltaZ) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }

        return rounded.set(rx, rz, 0);
    }

    /**
     * Convert axial vector to cube coordinates.
     *
     * @param axialPoint a point in axial coordinate space
     * @return same axialPoint reference, after conversion to cube coordinates
     */
    public static Vector3f axialToCube(Vector3f axialPoint) {
        return axialPoint.set(axialPoint.x(), -axialPoint.x() - axialPoint.y(), axialPoint.y());
    }
    /**
     * Convert a vector containing cartesian coordinates to their equivalent axial coordinates. The
     * conversion is done in place, so make sure that you no longer need the vector containing the
     * cartesian coordinates.
     *
     * @param cartesian Vector3f with cartesian coordinates to convert
     */

    public static void cartesianToAxial(Vector3f cartesian) {
        // Multiply cartesian by the PIXEL_TO_HEX matrix
        cartesian.mul(WORLD_TO_HEX);

        // And then divide both q and r by HEX_SIZE
        cartesian.x /= HEX_SIZE;
        cartesian.y /= HEX_SIZE;
        cartesian.z = 0;
    }
}
