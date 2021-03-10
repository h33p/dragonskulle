/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import lombok.Builder;

/**
 * Describes layout of all attributes on the vertex shader
 *
 * @author Aurimas Blažulionis
 */
@Builder
public class AttributeDescription {
    public int bindingID;
    public int location;
    public int format;
    public int offset;

    public static final int MATRIX_ROW_SIZE = 4 * 4;
    public static final int MATRIX_SIZE = MATRIX_ROW_SIZE * 4;

    /**
     * Creates attribute descriptiions by appending the initial transformation matrix to the start.
     *
     * @param descriptions individual attribute descriptions. Their binding IDs will always be
     *     ignored, locations will be shifted by 7 to accomodate for per vertex, and the
     *     transformation matrix data, while offset will be shifted by 64. So, {@code bindingID} can
     *     be ignored, {@code location} should start at 0, and {@code offset} should do as well.
     *     Inside the shader, {@code location} will start from 7.
     */
    public static AttributeDescription[] withMatrix(AttributeDescription... descriptions) {
        AttributeDescription[] ret = new AttributeDescription[4 + descriptions.length];
        for (int i = 0; i < 4; i++)
            ret[i] =
                    new AttributeDescription(
                            1, i + 3, VK_FORMAT_R32G32B32A32_SFLOAT, i * MATRIX_ROW_SIZE);
        for (int i = 0; i < descriptions.length; i++)
            ret[i + 4] =
                    new AttributeDescription(
                            1, i + 7, descriptions[i].format, MATRIX_SIZE + descriptions[i].offset);
        return ret;
    }
}
