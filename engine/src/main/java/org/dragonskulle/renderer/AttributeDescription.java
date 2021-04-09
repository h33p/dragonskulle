/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Describes layout of all attributes on the vertex shader
 *
 * @author Aurimas Blažulionis
 */
public class AttributeDescription {
    public int bindingID;
    public int location;
    public int format;
    public int offset;

    public static final int MATRIX_ROW_SIZE = 4 * 4;
    public static final int MATRIX_SIZE = MATRIX_ROW_SIZE * 4;
    public static final int LIGHT_HALF_SIZE = 4 * 3;

    /** Constructor for AttributeDescription */
    public AttributeDescription(int bindingID, int location, int format, int offset) {
        this.bindingID = bindingID;
        this.location = location;
        this.format = format;
        this.offset = offset;
    }

    /**
     * Creates attribute descriptions by appending the initial transformation matrix to the start.
     *
     * @param descriptions individual attribute descriptions. Their binding IDs will always be
     *     ignored, locations will be shifted by 4 to accomodate for the transformation matrix data,
     *     while offset will be shifted by 64. So, {@code bindingID} can be ignored, {@code
     *     location} should start at 0, and {@code offset} should do as well. Inside the shader,
     *     {@code location} will start from 8 (since there is an implicit 4 shift due to per-vertex
     *     data).
     */
    public static AttributeDescription[] withMatrix(AttributeDescription... descriptions) {
        AttributeDescription[] ret = new AttributeDescription[4 + descriptions.length];
        for (int i = 0; i < 4; i++)
            ret[i] =
                    new AttributeDescription(
                            1, i, VK_FORMAT_R32G32B32A32_SFLOAT, i * MATRIX_ROW_SIZE);
        for (int i = 0; i < descriptions.length; i++)
            ret[i + 4] =
                    new AttributeDescription(
                            1, i + 4, descriptions[i].format, MATRIX_SIZE + descriptions[i].offset);
        return ret;
    }

    /**
     * Creates attribute descriptions by appending lights to the start.
     *
     * @param lightCount number of lights to be added. In total, 2 * lightCount locations will be
     *     consumed.
     * @param descriptions individual attribute descriptions. Their binding IDs will be ignored and
     *     set to 1. Their locations will be shifted by {@code lightCount * 2} to accomodate for the
     *     lights. {@code location} should start at 0, and {@code offset} should do as well. Inside
     *     the shader, {@code location} will start at {@code lightCount * 2 + 4} (since there is an
     *     implicit 4 shift for vertex data), unless shifted by any of the other functions.
     */
    public static AttributeDescription[] withLights(
            int lightCount, AttributeDescription... descriptions) {
        AttributeDescription[] ret = new AttributeDescription[descriptions.length + 2 * lightCount];

        int lightSizes = LIGHT_HALF_SIZE * lightCount * 2;

        for (int i = 0; i < lightCount * 2; i++) {
            ret[i] =
                    new AttributeDescription(1, i, VK_FORMAT_R32G32B32_SFLOAT, i * LIGHT_HALF_SIZE);
        }

        for (int i = 0; i < descriptions.length; i++) {
            ret[i + lightCount * 2] =
                    new AttributeDescription(
                            1,
                            i + lightCount * 2,
                            descriptions[i].format,
                            lightSizes + descriptions[i].offset);
        }

        return ret;
    }
}
