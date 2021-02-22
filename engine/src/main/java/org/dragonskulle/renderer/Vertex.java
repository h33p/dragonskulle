/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import lombok.Builder;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

@Builder
public class Vertex {
    public static int SIZEOF = (2 + 3 + 2) * 4;
    public static int OFFSETOF_POS = 0;
    public static int OFFSETOF_COL = 2 * 4;
    public static int OFFSETOF_UV = OFFSETOF_COL + 3 * 4;

    private Vector2fc pos;
    private Vector3fc color;
    private Vector2fc uv;

    public void copyTo(ByteBuffer buffer) {
        buffer.putFloat(pos.x());
        buffer.putFloat(pos.y());

        buffer.putFloat(color.x());
        buffer.putFloat(color.y());
        buffer.putFloat(color.z());

        buffer.putFloat(uv.x());
        buffer.putFloat(uv.y());
    }

    public static VkVertexInputBindingDescription.Buffer getBindingDescription(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.callocStack(1, stack);
        bindingDescription.binding(0);
        bindingDescription.stride(SIZEOF);
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        return bindingDescription;
    }

    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(
            MemoryStack stack) {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.callocStack(3, stack);
        VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
        posDescription.binding(0);
        posDescription.location(0);
        posDescription.format(VK_FORMAT_R32G32_SFLOAT);
        posDescription.offset(OFFSETOF_POS);

        VkVertexInputAttributeDescription colDescription = attributeDescriptions.get(1);
        colDescription.binding(0);
        colDescription.location(1);
        colDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        colDescription.offset(OFFSETOF_COL);

        VkVertexInputAttributeDescription uvDescription = attributeDescriptions.get(2);
        uvDescription.binding(0);
        uvDescription.location(2);
        uvDescription.format(VK_FORMAT_R32G32_SFLOAT);
        uvDescription.offset(OFFSETOF_UV);

        return attributeDescriptions;
    }
}
