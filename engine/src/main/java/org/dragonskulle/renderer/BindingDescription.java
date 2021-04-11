/* (C) 2021 DragonSkulle */

package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_INSTANCE;

import lombok.Builder;

/**
 * Describes data layout of a vertex shader binding.
 *
 * @author Aurimas Blažulionis
 */
@Builder
public class BindingDescription {
    public int mbindingId;
    public int mSize;
    public int mInputRate;

    /** Creates a new binding description without default transformation matrix. */
    public static BindingDescription instanced(int size) {
        return new BindingDescription(1, size, VK_VERTEX_INPUT_RATE_INSTANCE);
    }

    /**
     * Creates a new binding description with default transformation matrix attached at the
     * beginning of the structure.
     */
    public static BindingDescription instancedWithMatrix(int size) {
        return instanced(size + AttributeDescription.MATRIX_SIZE);
    }

    /**
     * Creates a new binding description with default transformation matrix attached at the
     * beginning of the structure, and light information after it.
     */
    public static BindingDescription instancedWithMatrixAndLights(int size, int lightCount) {
        return instanced(
                size
                        + AttributeDescription.MATRIX_SIZE
                        + lightCount * 2 * AttributeDescription.LIGHT_HALF_SIZE);
    }
}
