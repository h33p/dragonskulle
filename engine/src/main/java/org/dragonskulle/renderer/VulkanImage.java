/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.*;

class VulkanImage {
    public long image;
    public long memory;

    public void destroyImage(VkDevice device) {
        vkDestroyImage(device, image, null);
        vkFreeMemory(device, memory, null);
    }
}
