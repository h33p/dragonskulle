/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

/**
 * Class abstracting a vulkan image memory
 *
 * @author Aurimas Blažulionis
 */
class VulkanImage implements NativeResource {
    public long image;
    public long memory;

    private VkDevice mDevice;

    private VulkanBuffer mStagingBuffer;

    public VulkanImage(
            Texture texture,
            VkCommandBuffer commandBuffer,
            VkDevice device,
            PhysicalDevice physicalDevice) {

        this(
                device,
                physicalDevice,
                texture.getWidth(),
                texture.getHeight(),
                VK_FORMAT_R8G8B8A8_SRGB,
                VK_IMAGE_TILING_OPTIMAL,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        try (MemoryStack stack = stackPush()) {
            mStagingBuffer =
                    new VulkanBuffer(
                            device,
                            physicalDevice,
                            texture.size(),
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(device, mStagingBuffer.memory, 0, texture.size(), 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) texture.size());
            byteBuffer.put(texture.getBuffer());
            texture.getBuffer().rewind();
            byteBuffer.rewind();
            vkUnmapMemory(device, mStagingBuffer.memory);

            transitionImageLayout(
                    commandBuffer,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyFromBuffer(mStagingBuffer, commandBuffer, texture.getWidth(), texture.getHeight());

            transitionImageLayout(
                    commandBuffer,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
    }

    public VulkanImage(
            VkDevice device,
            PhysicalDevice physicalDevice,
            int width,
            int height,
            int format,
            int tiling,
            int usage,
            int properties) {
        mDevice = device;
        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer pImage = stack.longs(0);

            int res = vkCreateImage(mDevice, imageInfo, null, pImage);

            if (res != VK_SUCCESS)
                throw new RuntimeException(String.format("Failed to create image! Res: %x", -res));

            this.image = pImage.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
            vkGetImageMemoryRequirements(mDevice, this.image, memoryRequirements);

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

            this.memory = pBufferMemory.get(0);

            res = vkBindImageMemory(mDevice, this.image, this.memory, 0);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to bind image memory! Ret: %x", -res));
            }
        }
    }

    public void freeStagingBuffer() {
        if (mStagingBuffer != null) {
            mStagingBuffer.free();
            mStagingBuffer = null;
        }
    }

    @Override
    public void free() {
        vkDestroyImage(mDevice, image, null);
        vkFreeMemory(mDevice, memory, null);
        freeStagingBuffer();
    }

    private void transitionImageLayout(
            VkCommandBuffer commandBuffer, long format, int oldLayout, int newLayout) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().layerCount(1);

            int srcStage = 0;
            int dstStage = 0;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                    && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                throw new RuntimeException("Unsupported layout transition!");
            }

            vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, barrier);
        }
    }

    private void copyFromBuffer(
            VulkanBuffer buffer, VkCommandBuffer commandBuffer, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);

            // offsets set to 0

            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().layerCount(1);
            region.imageSubresource().baseArrayLayer(0);

            region.imageExtent().width(width);
            region.imageExtent().height(height);
            region.imageExtent().depth(1);

            vkCmdCopyBufferToImage(
                    commandBuffer,
                    buffer.buffer,
                    image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);
        }
    }
}
