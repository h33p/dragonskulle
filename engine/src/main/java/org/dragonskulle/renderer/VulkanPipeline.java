/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.*;

import lombok.Builder;

@Builder
class VulkanPipeline {
    long pipeline;
    long layout;

    public void free(VkDevice device) {
        vkDestroyPipeline(device, pipeline, null);
        vkDestroyPipelineLayout(device, layout, null);
    }
}
