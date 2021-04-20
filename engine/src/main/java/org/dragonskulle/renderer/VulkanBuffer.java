/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkCreateBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements;

import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

/**
 * Class abstracting a vulkan buffer memory.
 *
 * @author Aurimas Blažulionis
 */
class VulkanBuffer implements NativeResource {
    public long mBuffer;
    public long mMemory;

    private VkDevice mDevice;

    public VulkanBuffer(
            VkDevice device, PhysicalDevice physicalDevice, long size, int usage, int properties) {
        mDevice = device;
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo createInfo = VkBufferCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            createInfo.size(size);
            createInfo.usage(usage);
            createInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.longs(0);

            int res = vkCreateBuffer(mDevice, createInfo, null, pBuffer);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to create buffer! Ret: %x", -res));
            }

            this.mBuffer = pBuffer.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
            vkGetBufferMemoryRequirements(mDevice, this.mBuffer, memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.callocStack(stack);
            allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocateInfo.allocationSize(memoryRequirements.size());
            allocateInfo.memoryTypeIndex(
                    physicalDevice.findMemoryType(memoryRequirements.memoryTypeBits(), properties));

            LongBuffer pBufferMemory = stack.longs(0);

            res = vkAllocateMemory(mDevice, allocateInfo, null, pBufferMemory);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to allocate buffer memory! Ret: %x", -res));
            }

            this.mMemory = pBufferMemory.get(0);

            res = vkBindBufferMemory(mDevice, this.mBuffer, this.mMemory, 0);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to bind buffer memory! Ret: %x", -res));
            }
        }
    }

    public void copyTo(VkCommandBuffer commandBuffer, VulkanBuffer to, long size) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.srcOffset(0);
            copyRegion.dstOffset(0);
            copyRegion.size(size);
            vkCmdCopyBuffer(commandBuffer, this.mBuffer, to.mBuffer, copyRegion);
        }
    }

    @Override
    public void free() {
        vkDestroyBuffer(mDevice, mBuffer, null);
        vkFreeMemory(mDevice, mMemory, null);
    }
}
