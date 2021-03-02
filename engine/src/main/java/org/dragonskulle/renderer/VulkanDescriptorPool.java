/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

/**
 * Class abstracting a vulkan descriptor pool and their sets
 *
 * <p>Normally, one pool is created per material/pipeline, unless multiple passes are rendered.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class VulkanDescriptorPool implements NativeResource {

    @Builder
    private static class DescriptorBinding {
        public int bindingID;
        public int size;
    }

    private VkDevice mDevice;
    private long mPool;
    @Getter private long mSetLayout;
    @Getter private long[] mDescriptorSets;

    private int mNumFragmentTextures;

    private VulkanBuffer mVertexUniformBuffers;
    private DescriptorBinding mVertexUniformBindingDescription;

    private VulkanBuffer mFragmentUniformBuffers;
    private DescriptorBinding mFragmentUniformBindingDescription;

    public VulkanDescriptorPool(
            VkDevice device,
            PhysicalDevice physicalDevice,
            IMaterial material,
            int descriptorCount) {
        mDevice = device;
        mPool = createDescriptorPool(material, descriptorCount);
        mSetLayout = createDescriptorSetLayoutAndBuffers(material, physicalDevice, descriptorCount);

        try (MemoryStack stack = stackPush()) {
            // Allocate descriptor sets
            LongBuffer setLayouts = stack.mallocLong(descriptorCount);
            IntStream.range(0, descriptorCount)
                    .mapToLong(__ -> mSetLayout)
                    .forEach(setLayouts::put);
            setLayouts.rewind();

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(mPool);
            allocInfo.pSetLayouts(setLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(descriptorCount);

            int res = vkAllocateDescriptorSets(mDevice, allocInfo, pDescriptorSets);

            if (res != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));

            mDescriptorSets =
                    IntStream.range(0, descriptorCount).mapToLong(pDescriptorSets::get).toArray();
        }
    }

    /**
     * Update descriptor sets
     *
     * <p>This method will simply update the attached descriptor set to have correct layout for
     * textures and uniform buffer.
     */
    public void updateDescriptorSets(VulkanSampledTexture[] textures) {
        for (int i = 0; i < mDescriptorSets.length; i++) updateDescriptorSet(i, textures);
    }

    public void updateDescriptorSet(int index, VulkanSampledTexture[] textures) {
        try (MemoryStack stack = stackPush()) {

            if (mNumFragmentTextures > 0
                    && (textures == null || textures.length != mNumFragmentTextures))
                throw new RuntimeException("Invalid texture argument passed!");

            long descriptorSet = mDescriptorSets[index];

            int bindingCount = mNumFragmentTextures;

            boolean hasFragmentUniform = mFragmentUniformBuffers != null;
            bindingCount += hasFragmentUniform ? 1 : 0;

            boolean hasVertexUniform = mVertexUniformBuffers != null;
            bindingCount += hasVertexUniform ? 1 : 0;

            VkWriteDescriptorSet.Buffer descriptorWrites =
                    VkWriteDescriptorSet.callocStack(bindingCount, stack);

            for (int i = mNumFragmentTextures - 1; i >= 0; i--) {
                VkDescriptorImageInfo.Buffer imageInfo =
                        VkDescriptorImageInfo.callocStack(1, stack);

                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(textures[i].getImageView());
                imageInfo.sampler(textures[i].getSampler());

                VkWriteDescriptorSet imageDescriptorWrite = descriptorWrites.get(--bindingCount);
                imageDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                imageDescriptorWrite.dstBinding(bindingCount);
                imageDescriptorWrite.dstArrayElement(0);
                imageDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                imageDescriptorWrite.descriptorCount(1);
                imageDescriptorWrite.pImageInfo(imageInfo);
                imageDescriptorWrite.dstSet(descriptorSet);
            }

            if (hasFragmentUniform) {
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(mFragmentUniformBindingDescription.size * index);
                bufferInfo.range(mFragmentUniformBindingDescription.size);
                bufferInfo.buffer(mFragmentUniformBuffers.buffer);

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(--bindingCount);
                uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                uboDescriptorWrite.dstBinding(mFragmentUniformBindingDescription.bindingID);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfo);
                uboDescriptorWrite.dstSet(descriptorSet);
            }

            if (hasVertexUniform) {
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(mVertexUniformBindingDescription.size * index);
                bufferInfo.range(mVertexUniformBindingDescription.size);
                bufferInfo.buffer(mVertexUniformBuffers.buffer);

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(--bindingCount);
                uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                uboDescriptorWrite.dstBinding(mVertexUniformBindingDescription.bindingID);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfo);
                uboDescriptorWrite.dstSet(descriptorSet);
            }

            vkUpdateDescriptorSets(mDevice, descriptorWrites, null);
        }
    }

    private long createDescriptorPool(IMaterial material, int descriptorCount) {
        Renderer.LOGGER.info("Setup descriptor pool");

        try (MemoryStack stack = stackPush()) {

            int numFragmentTextures = material.numFragmentTextures();

            int layoutCount = numFragmentTextures;

            boolean hasFragmentUniform = material.fragmentUniformDataSize() > 0;
            layoutCount += hasFragmentUniform ? 1 : 0;

            boolean hasVertexUniform = material.vertexUniformDataSize() > 0;
            layoutCount += hasVertexUniform ? 1 : 0;

            VkDescriptorPoolSize.Buffer poolSizes =
                    VkDescriptorPoolSize.callocStack(layoutCount, stack);

            for (int i = 0; i < numFragmentTextures; i++) {
                poolSizes.get(--layoutCount).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                poolSizes.get(layoutCount).descriptorCount(descriptorCount);
            }

            if (hasFragmentUniform) {
                poolSizes.get(--layoutCount).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                poolSizes.get(layoutCount).descriptorCount(descriptorCount);
            }

            if (hasVertexUniform) {
                poolSizes.get(--layoutCount).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                poolSizes.get(layoutCount).descriptorCount(descriptorCount);
            }

            if (layoutCount != 0) {
                throw new RuntimeException("BUG in VulkanDescriptorPool");
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(descriptorCount);

            LongBuffer pDescriptorPool = stack.longs(0);

            int res = vkCreateDescriptorPool(mDevice, poolInfo, null, pDescriptorPool);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor pool! Res: %x", -res));
            }

            return pDescriptorPool.get(0);
        }
    }

    /**
     * Create a descriptor set layout and uniform buffers
     *
     * <p>This layout is used in creating descriptor sets. It describes the properties shaders have
     * in different stages.
     */
    private long createDescriptorSetLayoutAndBuffers(
            IMaterial material, PhysicalDevice physicalDevice, int descriptorCount) {
        Renderer.LOGGER.info("Create descriptor set layout");

        try (MemoryStack stack = stackPush()) {

            mNumFragmentTextures = material.numFragmentTextures();

            int bindingCount = mNumFragmentTextures;

            int fragmentUniformDataSize = material.fragmentUniformDataSize();
            boolean hasFragmentUniform = fragmentUniformDataSize > 0;
            bindingCount += hasFragmentUniform ? 1 : 0;

            int vertexUniformDataSize = material.vertexUniformDataSize();
            boolean hasVertexUniform = vertexUniformDataSize > 0;
            bindingCount += hasVertexUniform ? 1 : 0;

            VkDescriptorSetLayoutBinding.Buffer layoutBindings =
                    VkDescriptorSetLayoutBinding.callocStack(bindingCount, stack);

            // The layout here is reversed, the last pool size is at offset 0

            for (int i = 0; i < mNumFragmentTextures; i++) {
                VkDescriptorSetLayoutBinding imageLayoutBinding =
                        layoutBindings.get(--bindingCount);
                imageLayoutBinding.binding(bindingCount);
                imageLayoutBinding.descriptorCount(1);
                imageLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                imageLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            }

            if (hasFragmentUniform) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = layoutBindings.get(--bindingCount);
                uboLayoutBinding.binding(bindingCount);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

                mFragmentUniformBuffers =
                        new VulkanBuffer(
                                mDevice,
                                physicalDevice,
                                fragmentUniformDataSize * descriptorCount,
                                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

                mFragmentUniformBindingDescription =
                        new DescriptorBinding(bindingCount, fragmentUniformDataSize);
            }

            if (hasVertexUniform) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = layoutBindings.get(--bindingCount);
                uboLayoutBinding.binding(bindingCount);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                mVertexUniformBuffers =
                        new VulkanBuffer(
                                mDevice,
                                physicalDevice,
                                vertexUniformDataSize * descriptorCount,
                                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

                mVertexUniformBindingDescription =
                        new DescriptorBinding(bindingCount, vertexUniformDataSize);
            }

            if (bindingCount != 0) {
                throw new RuntimeException("BUG in VulkanDescriptorPool");
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(layoutBindings);

            LongBuffer pDescriptorSetLayout = stack.longs(0);

            int res = vkCreateDescriptorSetLayout(mDevice, layoutInfo, null, pDescriptorSetLayout);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create descriptor set layout! Res: %x", -res));
            }

            return pDescriptorSetLayout.get(0);
        }
    }

    @Override
    public void free() {

        if (mFragmentUniformBuffers != null) mFragmentUniformBuffers.free();
        mFragmentUniformBuffers = null;

        if (mVertexUniformBuffers != null) mVertexUniformBuffers.free();
        mVertexUniformBuffers = null;

        vkDestroyDescriptorPool(mDevice, mPool, null);
        vkDestroyDescriptorSetLayout(mDevice, mSetLayout, null);

        mDescriptorSets = null;
    }
}
