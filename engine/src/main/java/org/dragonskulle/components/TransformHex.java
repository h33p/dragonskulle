/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Engine;
import org.joml.AxisAngle4f;
import org.joml.Matrix2f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Represents an objects position in hex coordinates
 *
 * @author Harry Stoltz
 *     <p>This extends Transform, and provides transformations to be done in hex (axial)
 *     coordinates.
 */
@Accessors(prefix = "m")
public class TransformHex extends Transform {
    public static final float HEX_SIZE = 1f;
    public static final float HEX_WIDTH = (float) Math.sqrt(3) * HEX_SIZE;
    public static final float HEX_HEIGHT = 2 * HEX_SIZE;

    // Matrix that takes a cartesian coordinate into hex coordinate space
    public static final Matrix2f WORLD_TO_HEX =
            new Matrix2f((float) Math.sqrt(3) / 3, 0, -1f / 3f, 2f / 3f);

    // Matrix that takes an axial coordinate into cartesian coordinate space
    // It's a 3x3 matrix so that a 3d vector can still be multiplied
    public static final Matrix3f HEX_TO_WORLD =
            new Matrix3f((float) Math.sqrt(3), 0, 0, (float) Math.sqrt(3) / 2, 3f / 2f, 0, 0, 0, 0);

    private Vector2f mPosition = new Vector2f();

    private float mRotation = 0f;
    @Getter private float mHeight = 0f;

    private Vector3f mTmp3DVec = new Vector3f();
    private Matrix4f mWorldMatrix = new Matrix4f();
    private Matrix4f mLocalMatrix = new Matrix4f();

    static {
        Engine.getCloner()
                .registerFastCloner(
                        TransformHex.class,
                        (t, cloner, clones) -> {
                            TransformHex toClone = (TransformHex) t;
                            TransformHex cloned =
                                    new TransformHex(
                                            toClone.mPosition.x,
                                            toClone.mPosition.y,
                                            toClone.mHeight);
                            cloned.mRotation = toClone.mRotation;
                            cloned.mGameObject = cloner.deepClone(toClone.mGameObject, clones);
                            return cloned;
                        });
    }

    /** Default constructor for TransformHex */
    public TransformHex() {}

    /**
     * Create a new Transform from Hex coordinates. This can be used for instantiating GameObjects
     * at a given Hex position
     *
     * @param axial The axial coordinates for the transform
     */
    public TransformHex(Vector2fc axial) {
        setPosition(axial);
    }

    /**
     * Create a new Transform for hex coordinates.
     *
     * @param q q axial coordinate
     * @param r r axial coordinate
     * @param height height of the object above the plane
     */
    public TransformHex(float q, float r, float height) {
        setPosition(q, r);
        mHeight = height;
    }

    /**
     * Create a new Transform for hex coordinates.
     *
     * @param q q axial coordinate
     * @param r r axial coordinate
     */
    public TransformHex(float q, float r) {
        setPosition(q, r);
    }

    /**
     * Translate the object locally
     *
     * @param axial axial coordinates to translate the object with on the transformation plane
     */
    public void translate(Vector2fc axial) {
        translate(axial, 0f);
    }

    /**
     * Translate the object locally
     *
     * @param axial axial coordinates to translate the object with on the transformation plane
     * @param height vertical transformation
     */
    public void translate(Vector2fc axial, float height) {
        translate(axial.x(), axial.y(), height);
    }

    /**
     * Translate the object locally
     *
     * @param q q axial coordinate
     * @param r r axial coordinate
     */
    public void translate(float q, float r) {
        mPosition.add(q, r);
        setUpdateFlag();
    }

    /**
     * Translate the object locally with hex coordinates
     *
     * @param x x hex coordinate (+q)
     * @param y y hex coordinate (+q-r)
     * @param z z hex coordinate (-r)
     */
    public void translate(float x, float y, float z) {
        translate(x + y, -z - y);
    }

    /**
     * Translate the object locally
     *
     * @param height vertical height to translate
     */
    public void translate(float height) {
        mHeight += height;
        setUpdateFlag();
    }

    /**
     * Set the object's height
     *
     * @param height new height
     */
    public void setHeight(float height) {
        mHeight = height;
        setUpdateFlag();
    }

    /**
     * Set the position of the transform to the provided hex coordinates. The cartesian position
     * will be the centre point of the hex
     *
     * @param axial Coordinates of the position in axial coordinate system
     */
    public void setPosition(Vector2fc axial) {
        mPosition.set(axial);
        setUpdateFlag();
    }

    /**
     * Set the position of the transform to the provided hex coordinates. The cartesian position
     * will be the centre point of the hex
     *
     * @param q q coordinate in axial coordinate system
     * @param r r coordinate in axial coordinate system
     */
    public void setPosition(float q, float r) {
        mPosition.set(q, r);
        setUpdateFlag();
    }

    /**
     * Get the position of a hex in cube coordinates. The z component of the vector is -x-y
     *
     * @param dest matrix to write the position to
     * @return Vector3f containing the axial coordinates (dest)
     */
    public Vector3f getLocalPosition(Vector3f dest) {
        dest.set(mPosition.x, mPosition.y, -mPosition.x - mPosition.y);
        return dest;
    }

    /**
     * Get the position of a hex in axial coordinates, rounded to nearest integer coordinate. The z
     * component of the vector is always 0
     *
     * @param dest matrix to write the position to
     * @return Vector3f containing the axial coordinates (dest)
     */
    public Vector3f getRoundedLocalPosition(Vector3f dest) {
        return TransformHex.roundCube(axialToCube(mPosition.x, mPosition.y, dest));
    }

    /** Gets the world transformation matrix */
    @Override
    public Matrix4fc getWorldMatrix() {
        if (mShouldUpdate) {
            mShouldUpdate = false;

            axialToCartesian(mPosition, mHeight, mTmp3DVec);

            mLocalMatrix.identity().translate(mTmp3DVec).rotateZ(mRotation);

            Transform parent = mGameObject.getParentTransform();
            if (parent != null) mWorldMatrix.set(parent.getMatrixForChildren());
            else mWorldMatrix.identity();

            mWorldMatrix.mul(mLocalMatrix);
        }

        return mWorldMatrix;
    }

    /**
     * Sets the local 3D transformation
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
        mTmp3DVec.set(position);
        cartesianToAxial(mTmp3DVec, mPosition);
        mHeight = position.z();
        AxisAngle4f rotAxis = rotation.get(new AxisAngle4f());
        mRotation = rotAxis.z * rotAxis.angle;
        setUpdateFlag();
    }

    @Override
    public void onDestroy() {}

    /**
     * Convert a vector containing axial coordinates to their equivalent cartesian coordinates.
     *
     * @param axial Vector2fc with axial coordinates to convert
     * @param height height of the hexagon
     * @param cartesian Vector3f where the result will be written
     */
    public static void axialToCartesian(Vector2fc axial, float height, Vector3f cartesian) {
        // Multiply axial by the HEX_TO_PIXEL matrix
        cartesian.set(axial.x(), axial.y(), 0);
        cartesian.mul(HEX_TO_WORLD);

        // And then multiply q and r by HEX_SIZE
        cartesian.x *= HEX_SIZE;
        cartesian.y *= HEX_SIZE;
        cartesian.z = height;
    }

    /**
     * Gets the nearest hex from a fractional axial position
     *
     * @param axialPoint the fractional axial coordinate
     * @param cubeCoordsOut 3D vector that will store cube coordinates
     * @return Vector2f the input axial coordinate, rounded.
     */
    public static Vector2f roundAxial(Vector2f axialPoint, Vector3f cubeCoordsOut) {
        axialToCube(axialPoint.x, axialPoint.y, cubeCoordsOut);
        roundCube(cubeCoordsOut);
        return axialPoint.set(cubeCoordsOut.x, cubeCoordsOut.y);
    }

    /**
     * Get the nearest hex from a fractional hex position.
     *
     * <p>Algorithm from: https://www.redblobgames.com/grids/hexagons/#rounding
     *
     * @param cubePoint The fractional cube coordinate
     * @return Vector3f containing the actual hex coordinates
     */
    public static Vector3f roundCube(Vector3f cubePoint) {
        Vector3f rounded = cubePoint;

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

        return rounded.set(rx, ry, rz);
    }

    /**
     * Convert axial vector to cube coordinates
     *
     * @param q the q axial coordinate
     * @param r the r axial cooredinate
     * @param dest destination vector
     * @return same dest reference, after conversion to cube coordinates
     */
    public static Vector3f axialToCube(float q, float r, Vector3f dest) {
        return dest.set(q, r, -q - r);
    }

    /**
     * Convert axial vector to cube coordinates
     *
     * @param axialPoint a point in axial coordinate space
     * @param dest destination vector
     * @return same dest reference, after conversion to cube coordinates
     */
    public static Vector3f axialToCube(Vector2fc axialPoint, Vector3f dest) {
        return axialToCube(axialPoint.x(), axialPoint.y(), dest);
    }

    /**
     * Convert a vector containing cartesian coordinates to their equivalent axial coordinates. The
     * conversion is done in place, so make sure that you no longer need the vector containing the
     * cartesian coordinates
     *
     * @param cartesian Vector3f with cartesian coordinates to convert
     * @param axial Vector2f that will contain the converted coordinates
     * @return converted axial coordinates (the same {@code axial} reference)
     */
    public static Vector2f cartesianToAxial(Vector3fc cartesian, Vector2f axial) {
        axial.set(cartesian.x(), cartesian.y());
        // Multiply cartesian by the PIXEL_TO_HEX matrix
        axial.mul(WORLD_TO_HEX);

        // And then divide both q and r by HEX_SIZE
        axial.x /= HEX_SIZE;
        axial.y /= HEX_SIZE;
        return axial;
    }
}
