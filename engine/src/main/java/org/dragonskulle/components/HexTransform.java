/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Represents an objects position in hex coordinates
 *
 * @author Harry Stoltz
 *     <p>This class acts as a wrapper for Transform, and converts the 3d cartesian position,
 *     rotation, etc into hex coordinates. Also allows for transformations to be done in hex
 *     coordinates, which are converted into cartesian transformations and added to the underlying
 *     3d transform.
 */
@Accessors(prefix = "m")
public class HexTransform {

    // TODO: We need to decide upon a size for hexes, I've just chosen two randomly for now so that
    //          I can implement equations we need

    // TODO: This also assumes we're using a pointy-topped configuration. I don't know whether
    //          we're doing this or flat-topped
    public static final float HEX_SIZE = 2.f;
    public static final float HEX_WIDTH = (float) Math.sqrt(3) * HEX_SIZE;
    public static final float HEX_HEIGHT = 2 * HEX_SIZE;

    // Matrix that takes a cartesian coordinate into hex coordinate space
    // It's a 3x3 matrix so that a 3d vector can still be multiplied
    public static final Matrix3f WORLD_TO_HEX =
            new Matrix3f((float) Math.sqrt(3) / 3, 0, 0, -1f / 3f, 2f / 3f, 0, 0, 0, 0);

    public static final Matrix3f HEX_TO_WORLD =
            new Matrix3f((float) Math.sqrt(3), 0, 0, (float) Math.sqrt(3) / 2, 3f / 2f, 0, 0, 0, 0);

    @Getter private final Transform mTransform;

    /**
     * Convert a vector containing axial coordinates to their equivalent cartesian coordinates. The
     * conversion is done in place, so make sure that you no longer need the vector containing the
     * axial coordinates
     *
     * @param axial Vector3f with axial coordinates to convert
     */
    public static void axialToCartesian(Vector3f axial) {
        // Multiply axial by the HEX_TO_PIXEL matrix
        axial.mul(HEX_TO_WORLD);

        // And then multiply q and r by HEX_SIZE
        axial.x *= HEX_SIZE;
        axial.y *= HEX_SIZE;
        axial.z = 0;
    }

    /**
     * Convert a vector containing cartesian coordinates to their equivalent axial coordinates. The
     * conversion is done in place, so make sure that you no longer need the vector containing the
     * cartesian coordinates
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

    /**
     * Constructor if this is going to be used as a wrapper for an existing Transform
     *
     * @param transform The Transform this class is wrapping
     */
    public HexTransform(Transform transform) {
        mTransform = transform;
    }

    /**
     * Create a new Transform from Hex coordinates. This can be used for instantiating GameObjects
     * at a given Hex position
     *
     * @param axial The axial coordinates for the transform
     */
    public HexTransform(Vector3f axial) {
        mTransform = new Transform();
        axialToCartesian(axial);
        mTransform.setPosition(axial);
    }

    /**
     * Set the position of the transform to the provided hex coordinates. The cartesian position
     * will be the centre point of the hex
     *
     * @param axial Coordinates of the position in axial coordinate system
     */
    public void setPosition(Vector3f axial) {
        // Convert the axial coordinates to cartesian and then set the transform position
        axialToCartesian(axial);
        mTransform.setPosition(axial);
    }

    /**
     * Get the position of a hex in axial coordinates. The z component of the vector is always 0
     *
     * @return Vector3f containing the axial coordinates
     */
    public Vector3f getPosition() {
        // Get the 3d position of the hex
        Vector3f pos = new Vector3f();
        mTransform.getPosition(pos);

        cartesianToAxial(pos);

        return roundAxial(pos);
    }

    /**
     * Get the nearest hex from a fractional hex position.
     *
     * <p>Algorithm from: https://www.redblobgames.com/grids/hexagons/#rounding
     *
     * @param axialPoint The fractional axial coordinate
     * @return Vector3f containing the actual hex coordinates
     */
    private Vector3f roundAxial(Vector3f axialPoint) {

        // First convert the axial coordinates to cube:
        Vector3f rounded = new Vector3f(axialPoint.x, -axialPoint.x - axialPoint.y, axialPoint.y);

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
}
