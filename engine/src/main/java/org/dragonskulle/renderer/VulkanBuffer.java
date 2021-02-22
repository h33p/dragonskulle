/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.*;

class VulkanBuffer {
    public long buffer;
    public long memory;

    public void destroyBuffer(VkDevice device) {
        vkDestroyBuffer(device, buffer, null);
        vkFreeMemory(device, memory, null);
    }
}
