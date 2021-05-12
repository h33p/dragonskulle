/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D24_UNORM_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_STENCIL_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBindImageMemory;
import static org.lwjgl.vulkan.VK10.vkCmdBlitImage;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateImage;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetImageMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.utils.MathUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

/**
 * Class abstracting a vulkan image memory.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class VulkanImage implements NativeResource {
    /** Underlying handle to the image itself. */
    public long mImage;
    /** Underlying handle to the image memory. */
    public long mMemory;

    /** Image format. */
    private int mFormat;
    /** Image subresource range aspect mask. */
    private int mAspectMask;
    /** Current image layout. */
    private int mImageLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    /** Number of mipmap levels used. */
    @Getter private int mMipLevels = 1;
    /** Logical device used. */
    private VkDevice mDevice;

    /** Controls whether the handles on this image are released on free. */
    private boolean mNeedsFree = true;

    /** Temporary staging buffer for the image. */
    private VulkanBuffer mStagingBuffer;

    /**
     * Create a depth texture
     *
     * <p>Staging buffer should be freed after the command buffer is flushed/freed.
     *
     * @param commandBuffer command buffer to use to load this image.
     * @param device logical device to use.
     * @param physicalDevice physical device to use.
     * @param width width of the image.
     * @param height height of the image.
     * @param numSamples number of mipmap samples to use.
     * @return the newly created depth image.
     */
    public static VulkanImage createDepthImage(
            VkCommandBuffer commandBuffer,
            VkDevice device,
            PhysicalDevice physicalDevice,
            int width,
            int height,
            int numSamples)
            throws RendererException {
        return new VulkanImage(
                commandBuffer,
                device,
                physicalDevice,
                width,
                height,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                numSamples,
                physicalDevice.findDepthFormat(),
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
    }

    /**
     * Create a VulkanImage with provided image
     *
     * <p>This method is provided for swapchain images, that do not need to be freed manually.
     *
     * @param device logical device to use.
     * @param format target image format.
     * @param image on-GPU image.
     */
    public VulkanImage(VkDevice device, int format, long image) {
        this.mDevice = device;
        this.mFormat = format;
        this.mImage = image;
        this.mAspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        this.mNeedsFree = false;
    }

    /**
     * Create a vulkan image.
     *
     * @param commandBuffer command buffer to use to load this image.
     * @param device logical device to use.
     * @param physicalDevice physical device to use.
     * @param width width of the image.
     * @param height height of the image.
     * @param usage target usage of the image.
     * @param numSamples number of mipmap samples to use.
     * @param format target image format.
     * @param aspectMask subresource range aspect mask.
     * @param targetLayout target layout of the image.
     */
    public VulkanImage(
            VkCommandBuffer commandBuffer,
            VkDevice device,
            PhysicalDevice physicalDevice,
            int width,
            int height,
            int usage,
            int numSamples,
            int format,
            int aspectMask,
            int targetLayout)
            throws RendererException {
        this(
                device,
                physicalDevice,
                width,
                height,
                format,
                aspectMask,
                numSamples,
                VK_IMAGE_TILING_OPTIMAL,
                usage,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        transitionImageLayout(commandBuffer, targetLayout);
    }

    /**
     * Create a vulkan image.
     *
     * @param texture texture to load.
     * @param linearData whether this texture is linear or sRGB.
     * @param commandBuffer command buffer to use to load this image.
     * @param device logical device to use.
     * @param physicalDevice physical device to use.
     */
    public VulkanImage(
            Texture texture,
            boolean linearData,
            VkCommandBuffer commandBuffer,
            VkDevice device,
            PhysicalDevice physicalDevice)
            throws RendererException {

        this(
                device,
                physicalDevice,
                texture.getWidth(),
                texture.getHeight(),
                linearData ? VK_FORMAT_R8G8B8A8_UNORM : VK_FORMAT_R8G8B8A8_SRGB,
                VK_IMAGE_ASPECT_COLOR_BIT,
                VK_SAMPLE_COUNT_1_BIT,
                VK_IMAGE_TILING_OPTIMAL,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT
                        | VK_IMAGE_USAGE_TRANSFER_SRC_BIT
                        | VK_IMAGE_USAGE_SAMPLED_BIT,
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
            vkMapMemory(device, mStagingBuffer.mMemory, 0, texture.size(), 0, pData);
            ByteBuffer byteBuffer = pData.getByteBuffer((int) texture.size());
            byteBuffer.put(texture.getBuffer());
            texture.getBuffer().rewind();
            byteBuffer.rewind();
            vkUnmapMemory(device, mStagingBuffer.mMemory);

            transitionImageLayout(commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyFromBuffer(mStagingBuffer, commandBuffer, texture.getWidth(), texture.getHeight());

            generateMipmaps(
                    commandBuffer,
                    physicalDevice,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    texture.getWidth(),
                    texture.getHeight());
        }
    }

    /**
     * Generate mipmaps for the image.
     *
     * @param commandBuffer command buffer to write the commands to.
     * @param physDev physical device to use.
     * @param format target image format.
     * @param width width of the image.
     * @param height height of the image.
     */
    private void generateMipmaps(
            VkCommandBuffer commandBuffer,
            PhysicalDevice physDev,
            int format,
            int width,
            int height) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.image(mImage);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.subresourceRange().aspectMask(mAspectMask);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);
            barrier.subresourceRange().levelCount(1);

            VkImageBlit.Buffer blit = VkImageBlit.callocStack(1, stack);

            int twidth = width;
            int theight = height;

            for (int i = 0; i < mMipLevels - 1; i++) {
                barrier.subresourceRange().baseMipLevel(i);

                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        null,
                        null,
                        barrier);

                blit.srcOffsets(0).clear();
                blit.srcOffsets(1).set(twidth, theight, 1);
                blit.srcSubresource().aspectMask(mAspectMask);
                blit.srcSubresource().mipLevel(i);
                blit.srcSubresource().baseArrayLayer(0);
                blit.srcSubresource().layerCount(1);
                blit.dstOffsets(0).clear();
                blit.dstOffsets(1)
                        .set(twidth > 1 ? twidth / 2 : 1, theight > 1 ? theight / 2 : 1, 1);
                blit.dstSubresource().aspectMask(mAspectMask);
                blit.dstSubresource().mipLevel(i + 1);
                blit.dstSubresource().baseArrayLayer(0);
                blit.dstSubresource().layerCount(1);

                vkCmdBlitImage(
                        commandBuffer,
                        mImage,
                        VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        mImage,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit,
                        physDev.getFeatureSupport().mOptimalLinearTiling
                                ? VK_FILTER_LINEAR
                                : VK_FILTER_NEAREST);

                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        0,
                        null,
                        null,
                        barrier);

                if (twidth > 1) {
                    twidth /= 2;
                }
                if (theight > 1) {
                    theight /= 2;
                }
            }

            barrier.subresourceRange().baseMipLevel(mMipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            vkCmdPipelineBarrier(
                    commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0,
                    null,
                    null,
                    barrier);
        }
    }

    /**
     * Create a vulkan image.
     *
     * @param device logical device to use.
     * @param physicalDevice physical device to use.
     * @param width width of the image.
     * @param height height of the image.
     * @param format image format.
     * @param aspectMask subresource range aspect mask.
     * @param numSamples number of mipmap samples to use.
     * @param tiling target tiling of the image.
     * @param usage how is this image used.
     * @param properties target memory type properties.
     */
    public VulkanImage(
            VkDevice device,
            PhysicalDevice physicalDevice,
            int width,
            int height,
            int format,
            int aspectMask,
            int numSamples,
            int tiling,
            int usage,
            int properties)
            throws RendererException {
        mDevice = device;
        mFormat = format;
        mAspectMask = aspectMask;
        mMipLevels =
                (usage & VK_IMAGE_USAGE_TRANSFER_SRC_BIT) == 0
                        ? 1
                        : MathUtils.log(Math.max(width, height), 2) + 1;

        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(mMipLevels);
            imageInfo.arrayLayers(1);
            imageInfo.format(mFormat);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(numSamples);

            LongBuffer pImage = stack.longs(0);

            int res = vkCreateImage(mDevice, imageInfo, null, pImage);

            if (res != VK_SUCCESS) {
                throw new RendererException(String.format("Failed to create image! Res: %x", -res));
            }

            this.mImage = pImage.get(0);

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
            vkGetImageMemoryRequirements(mDevice, this.mImage, memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.callocStack(stack);
            allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocateInfo.allocationSize(memoryRequirements.size());
            allocateInfo.memoryTypeIndex(
                    physicalDevice.findMemoryType(memoryRequirements.memoryTypeBits(), properties));

            LongBuffer pBufferMemory = stack.longs(0);

            res = vkAllocateMemory(mDevice, allocateInfo, null, pBufferMemory);

            if (res != VK_SUCCESS) {
                throw new RendererException(
                        String.format("Failed to allocate buffer memory! Ret: %x", -res));
            }

            this.mMemory = pBufferMemory.get(0);

            res = vkBindImageMemory(mDevice, this.mImage, this.mMemory, 0);

            if (res != VK_SUCCESS) {
                throw new RendererException(
                        String.format("Failed to bind image memory! Ret: %x", -res));
            }
        }
    }

    /**
     * Creates a image view for an image
     *
     * <p>Image views are needed to render to/read from.
     *
     * @return newly created image view.
     * @throws RendererException if there is a failure creating image view.
     */
    public long createImageView() throws RendererException {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            createInfo.format(mFormat);

            createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

            createInfo.subresourceRange().aspectMask(mAspectMask);
            createInfo.subresourceRange().baseMipLevel(0);
            createInfo.subresourceRange().levelCount(mMipLevels);
            createInfo.subresourceRange().baseArrayLayer(0);
            createInfo.subresourceRange().layerCount(1);

            createInfo.image(mImage);

            LongBuffer imageView = stack.longs(0);

            int result = vkCreateImageView(mDevice, createInfo, null, imageView);
            if (result != VK_SUCCESS) {
                throw new RendererException(
                        String.format(
                                "Failed to create image view for %x! Error: %x", mImage, -result));
            }

            return imageView.get(0);
        }
    }

    /** Free the staging buffer. */
    public void freeStagingBuffer() {
        if (mStagingBuffer != null) {
            mStagingBuffer.free();
            mStagingBuffer = null;
        }
    }

    /** Must free the image, if it was created manually. */
    @Override
    public void free() {
        if (mNeedsFree) {
            if (mImage != 0) {
                vkDestroyImage(mDevice, mImage, null);
                mImage = 0;
            }
            if (mMemory != 0) {
                vkFreeMemory(mDevice, mMemory, null);
                mMemory = 0;
            }
            freeStagingBuffer();
        }
    }

    /**
     * Check whether the image format has stencil component.
     *
     * @return {@code true} if the image has the stencil component. {@code false} otherwise.
     */
    private boolean hasStencilComponent() {
        return mFormat == VK_FORMAT_D32_SFLOAT_S8_UINT || mFormat == VK_FORMAT_D24_UNORM_S8_UINT;
    }

    /**
     * Transition image layout to a new one.
     *
     * @param commandBuffer command buffer to use.
     * @param newLayout target image layout.
     */
    private void transitionImageLayout(VkCommandBuffer commandBuffer, int newLayout)
            throws RendererException {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(mImageLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(mImage);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mMipLevels);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().layerCount(1);

            if (newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                mAspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;

                if (hasStencilComponent()) {
                    mAspectMask |= VK_IMAGE_ASPECT_STENCIL_BIT;
                }
            }

            barrier.subresourceRange().aspectMask(mAspectMask);

            int srcStage = 0;
            int dstStage = 0;

            if (mImageLayout == VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (mImageLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                    && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

            } else if (mImageLayout == VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(
                        VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT
                                | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

            } else if (mImageLayout == VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(
                        VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            } else {
                throw new RendererException("Unsupported layout transition!");
            }

            vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, barrier);
            mImageLayout = newLayout;
        }
    }

    /**
     * Copy image from one a buffer.
     *
     * @param buffer buffer to copy from.
     * @param commandBuffer buffer for commands.
     * @param width width of the image.
     * @param height height of the image.
     */
    private void copyFromBuffer(
            VulkanBuffer buffer, VkCommandBuffer commandBuffer, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);

            // offsets set to 0

            region.imageSubresource().aspectMask(mAspectMask);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().layerCount(1);
            region.imageSubresource().baseArrayLayer(0);

            region.imageExtent().width(width);
            region.imageExtent().height(height);
            region.imageExtent().depth(1);

            vkCmdCopyBufferToImage(
                    commandBuffer,
                    buffer.mBuffer,
                    mImage,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);
        }
    }
}
