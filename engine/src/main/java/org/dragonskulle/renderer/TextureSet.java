/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

/**
 * Class abstracting a set of textures and their descriptor sets
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class TextureSet implements NativeResource {

    @Getter private VulkanSampledTexture[] mTextures;

    private VkDevice mDevice;

    /** Descriptor pool of this TextureSet */
    @Getter private long mPool;
    /** Layout of the descriptor sets */
    @Getter private long mSetLayout;

    private long[] mDescriptorSets;

    public static final Logger LOGGER = Logger.getLogger("render");

    /**
     * Create a new TextureSet
     *
     * @param device current vulkan device
     * @param factory factory of texture set layouts
     * @param textures textures used
     * @param descriptorSetCount number of descriptor sets to create
     */
    public TextureSet(
            VkDevice device,
            TextureSetLayoutFactory factory,
            VulkanSampledTexture[] textures,
            int descriptorSetCount) {
        mTextures = Arrays.copyOf(textures, textures.length);
        mSetLayout = factory.getLayout(textures.length);

        mDevice = device;
        mPool = createDescriptorPool(textures.length, descriptorSetCount);

        try (MemoryStack stack = stackPush()) {
            // Allocate descriptor sets
            LongBuffer setLayouts = stack.mallocLong(descriptorSetCount);
            IntStream.range(0, descriptorSetCount)
                    .mapToLong(__ -> mSetLayout)
                    .forEach(setLayouts::put);
            setLayouts.rewind();

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(mPool);
            allocInfo.pSetLayouts(setLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(descriptorSetCount);

            int res = vkAllocateDescriptorSets(mDevice, allocInfo, pDescriptorSets);

            if (res != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));

            mDescriptorSets =
                    IntStream.range(0, descriptorSetCount)
                            .mapToLong(pDescriptorSets::get)
                            .toArray();

            for (int i = 0; i < mDescriptorSets.length; i++) {
                updateDescriptorSet(i);
            }
        }
    }

    /** Update the descriptor set to have correct textures attached */
    private void updateDescriptorSet(int index) {
        try (MemoryStack stack = stackPush()) {

            long descriptorSet = mDescriptorSets[index];

            VkWriteDescriptorSet.Buffer descriptorWrites =
                    VkWriteDescriptorSet.callocStack(mTextures.length, stack);

            for (int i = 0; i < mTextures.length; i++) {
                VkDescriptorImageInfo.Buffer imageInfo =
                        VkDescriptorImageInfo.callocStack(1, stack);

                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(mTextures[i].getImageView());
                imageInfo.sampler(mTextures[i].getSampler());

                VkWriteDescriptorSet imageDescriptorWrite = descriptorWrites.get(i);
                imageDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                imageDescriptorWrite.dstBinding(i);
                imageDescriptorWrite.dstArrayElement(0);
                imageDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                imageDescriptorWrite.descriptorCount(1);
                imageDescriptorWrite.pImageInfo(imageInfo);
                imageDescriptorWrite.dstSet(descriptorSet);
            }

            vkUpdateDescriptorSets(mDevice, descriptorWrites, null);
        }
    }

    /**
     * Create a new descriptor pool
     *
     * @param textureCount number of textures used
     * @param descriptorSetCount number of descriptor sets to create
     * @return the created descriptor pool
     */
    private long createDescriptorPool(int textureCount, int descriptorSetCount) {
        LOGGER.fine("Setup texture descriptor pool");

        try (MemoryStack stack = stackPush()) {

            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.callocStack(1, stack);

            poolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            poolSize.descriptorCount(textureCount * descriptorSetCount);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSize);
            poolInfo.maxSets(descriptorSetCount);

            LongBuffer pDescriptorPool = stack.longs(0);

            int res = vkCreateDescriptorPool(mDevice, poolInfo, null, pDescriptorPool);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor pool! Res: %x", -res));
            }

            return pDescriptorPool.get(0);
        }
    }

    /** Retrieve a descriptor set by image index */
    public long getDescriptorSet(int index) {
        return mDescriptorSets[index];
    }

    /** Free underlying descriptor pool */
    @Override
    public void free() {
        if (mPool != 0) vkDestroyDescriptorPool(mDevice, mPool, null);
        mPool = 0;
    }
}
