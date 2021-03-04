/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import lombok.Builder;

/**
 * Describes data layout of a vertex shader binding
 *
 * @author Aurimas Blažulionis
 */
@Builder
public class BindingDescription {
    public int bindingID;
    public int size;
    public int inputRate;

    /** Creates a new binding description without default transformation matrix */
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
}
