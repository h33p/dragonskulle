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
    /** Which binding the data is bound to. */
    public int mBindingId;
    /** The size of the data. */
    public int mSize;
    /** Controls at what rate the data is being inputted (per-vertex or per-instance). */
    public int mInputRate;

    /**
     * Creates a new binding description without default transformation matrix.
     *
     * @param size size of the data.
     * @return new binding description without matrix data.
     */
    public static BindingDescription instanced(int size) {
        return new BindingDescription(1, size, VK_VERTEX_INPUT_RATE_INSTANCE);
    }

    /**
     * Creates a new binding description with default transformation matrix attached at the
     * beginning of the structure.
     *
     * @param size size of the data.
     * @return new binding description with matrix size attached.
     */
    public static BindingDescription instancedWithMatrix(int size) {
        return instanced(size + AttributeDescription.MATRIX_SIZE);
    }

    /**
     * Creates a new binding description with default transformation matrix attached at the
     * beginning of the structure, and light information after it.
     *
     * @param size size of the data.
     * @param lightCount number of lights used.
     * @return binding description with additional data added.
     */
    public static BindingDescription instancedWithMatrixAndLights(int size, int lightCount) {
        return instanced(
                size
                        + AttributeDescription.MATRIX_SIZE
                        + lightCount * 2 * AttributeDescription.LIGHT_HALF_SIZE);
    }
}
