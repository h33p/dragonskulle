/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import lombok.Builder;
import org.dragonskulle.renderer.VulkanPipeline.AttributeDescription;
import org.dragonskulle.renderer.VulkanPipeline.BindingDescription;
import org.joml.*;

/**
 * Properties vertex shaders receive
 *
 * @author Aurimas Blažulionis
 */
@Builder
class UniformBufferObject {
    public static int SIZEOF = 4 * 4 * 4;
    public static int OFFSETOF_MODEL = 0;

    public Matrix4f model;

    public void copyTo(ByteBuffer buffer, int offset) {
        model.get(OFFSETOF_MODEL + offset, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer, 0);
    }

    public static final BindingDescription BINDING_DESCRIPTION =
            new BindingDescription(1, SIZEOF, VK_VERTEX_INPUT_RATE_INSTANCE);

    public static final AttributeDescription[] ATTRIBUTE_DESCRIPTIONS = {
        new AttributeDescription(1, 3, VK_FORMAT_R32G32B32A32_SFLOAT, OFFSETOF_MODEL),
        new AttributeDescription(1, 4, VK_FORMAT_R32G32B32A32_SFLOAT, OFFSETOF_MODEL + 16),
        new AttributeDescription(1, 5, VK_FORMAT_R32G32B32A32_SFLOAT, OFFSETOF_MODEL + 32),
        new AttributeDescription(1, 6, VK_FORMAT_R32G32B32A32_SFLOAT, OFFSETOF_MODEL + 48),
    };
}
