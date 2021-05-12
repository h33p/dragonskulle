/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_GEOMETRY_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

import java.nio.LongBuffer;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

/**
 * Class abstracting a vulkan descriptor pool and their sets for a single shader set
 *
 * <p>Normally, one pool is created per material/pipeline, unless multiple passes are rendered.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
class VulkanShaderDescriptorPool implements NativeResource {

    /** Describes a single desciptor binding. */
    @Builder
    private static class DescriptorBinding {
        /** Where the descriptor is bound. */
        public int mBindingId;
        /** Size of the binding. */
        public int mSize;
    }

    /** Describes a single descriptor set. */
    @Builder
    private static class DescriptorSetInfo {
        /** Which shader binding the descriptor is bound on. */
        DescriptorBinding mBindingDescriptor;
        /** Which uniform buffer the descriptor is on. */
        VulkanBuffer mUniformBuffer;
        /** At which shader stage the descriptor should be bound. */
        int mStageFlags;
    }

    /** Logical device. */
    private VkDevice mDevice;
    /** Allocated pool. */
    private long mPool;
    /** Descriptor set layout. */
    @Getter private long mSetLayout;
    /** Handles to allocated descriptor sets. */
    private long[] mDescriptorSets;

    /** Infos for each descriptor sets in the pool. */
    private DescriptorSetInfo[] mDescriptorSetInfos;

    /**
     * Create a descriptor pool.
     *
     * @param device logical device to use.
     * @param physicalDevice physical device to use.
     * @param shaderSet shader set to use.
     * @param descriptorCount number of descriptors to allocate.
     * @return vulkan descriptor pool for the input. {@code null} if there is a failure.
     * @throws RendererException if there is a severe error creating the pool.
     */
    public static VulkanShaderDescriptorPool createPool(
            VkDevice device,
            PhysicalDevice physicalDevice,
            ShaderSet shaderSet,
            int descriptorCount)
            throws RendererException {
        VulkanShaderDescriptorPool ret = new VulkanShaderDescriptorPool();
        ret.mDevice = device;
        ret.mPool = ret.createDescriptorPool(shaderSet, descriptorCount);

        if (ret.mPool == 0) {
            return null;
        }

        ret.mSetLayout =
                ret.createDescriptorSetLayoutAndBuffers(shaderSet, physicalDevice, descriptorCount);

        try (MemoryStack stack = stackPush()) {
            // Allocate descriptor sets
            LongBuffer setLayouts = stack.mallocLong(descriptorCount);
            IntStream.range(0, descriptorCount)
                    .mapToLong(__ -> ret.mSetLayout)
                    .forEach(setLayouts::put);
            setLayouts.rewind();

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(ret.mPool);
            allocInfo.pSetLayouts(setLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(descriptorCount);

            int res = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSets);

            if (res != VK_SUCCESS) {
                throw new RendererException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));
            }

            ret.mDescriptorSets =
                    IntStream.range(0, descriptorCount).mapToLong(pDescriptorSets::get).toArray();

            for (int i = 0; i < ret.mDescriptorSets.length; i++) {
                ret.updateDescriptorSet(i);
            }
        }

        return ret;
    }

    public long getDescriptorSet(int index) {
        return mDescriptorSets == null ? 0 : mDescriptorSets[index];
    }

    /**
     * Update a descriptor set
     *
     * <p>This method will simply update the attached descriptor set to have correct layout for
     * uniform buffers.
     */
    private void updateDescriptorSet(int index) {
        try (MemoryStack stack = stackPush()) {
            long descriptorSet = mDescriptorSets[index];

            VkWriteDescriptorSet.Buffer descriptorWrites =
                    VkWriteDescriptorSet.callocStack(mDescriptorSetInfos.length, stack);

            for (int i = 0; i < mDescriptorSetInfos.length; i++) {
                DescriptorSetInfo info = mDescriptorSetInfos[i];
                VkDescriptorBufferInfo.Buffer bufferInfo =
                        VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(info.mBindingDescriptor.mSize * index);
                bufferInfo.range(info.mBindingDescriptor.mSize);
                bufferInfo.buffer(info.mUniformBuffer.mBuffer);

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                uboDescriptorWrite.dstBinding(info.mBindingDescriptor.mBindingId);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfo);
                uboDescriptorWrite.dstSet(descriptorSet);
            }

            vkUpdateDescriptorSets(mDevice, descriptorWrites, null);
        }
    }

    /**
     * Create a descriptor pool
     *
     * @param shaderSet shader set to use.
     * @param descriptorCount number of descriptors to allocate.
     * @throws RendererException if there is an error creating the pool
     */
    private long createDescriptorPool(ShaderSet shaderSet, int descriptorCount)
            throws RendererException {
        log.fine("Setup descriptor pool");

        try (MemoryStack stack = stackPush()) {

            int layoutCount = shaderSet.numUniformBindings();

            if (layoutCount == 0) {
                return 0;
            }

            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.callocStack(1, stack);

            poolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            poolSize.descriptorCount(descriptorCount * layoutCount);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSize);
            poolInfo.maxSets(descriptorCount);

            LongBuffer pDescriptorPool = stack.longs(0);

            int res = vkCreateDescriptorPool(mDevice, poolInfo, null, pDescriptorPool);

            if (res != VK_SUCCESS) {
                throw new RendererException(
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
     *
     * @param shaderSet shader set to use.
     * @param physicalDevice physical device to use.
     * @param descriptorCount number of descriptors to create.
     * @throws RendererException if there is an error allocating memory
     */
    private long createDescriptorSetLayoutAndBuffers(
            ShaderSet shaderSet, PhysicalDevice physicalDevice, int descriptorCount)
            throws RendererException {
        log.fine("Create descriptor set layout");

        try (MemoryStack stack = stackPush()) {
            int bindingCount = shaderSet.numUniformBindings();

            mDescriptorSetInfos = new DescriptorSetInfo[bindingCount];

            int[][] uniformDataInfos = {
                {shaderSet.getVertexUniformDataSize(), VK_SHADER_STAGE_VERTEX_BIT},
                {shaderSet.getGeometryUniformDataSize(), VK_SHADER_STAGE_GEOMETRY_BIT},
                {shaderSet.getFragmentUniformDataSize(), VK_SHADER_STAGE_FRAGMENT_BIT}
            };

            int infoIndex = 0;

            for (int[] dataInfo : uniformDataInfos) {
                if (dataInfo[0] < 0) {
                    continue;
                }

                mDescriptorSetInfos[infoIndex++] =
                        new DescriptorSetInfo(
                                new DescriptorBinding(bindingCount, dataInfo[0]),
                                new VulkanBuffer(
                                        mDevice,
                                        physicalDevice,
                                        dataInfo[0] * descriptorCount,
                                        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                                | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
                                dataInfo[1]);
            }

            VkDescriptorSetLayoutBinding.Buffer layoutBindings =
                    VkDescriptorSetLayoutBinding.callocStack(mDescriptorSetInfos.length, stack);

            for (int i = 0; i < mDescriptorSetInfos.length; i++) {
                DescriptorSetInfo info = mDescriptorSetInfos[i];
                VkDescriptorSetLayoutBinding uboLayoutBinding = layoutBindings.get(i);
                uboLayoutBinding.binding(i);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.stageFlags(info.mStageFlags);
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo =
                    VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(layoutBindings);

            LongBuffer pDescriptorSetLayout = stack.longs(0);

            int res = vkCreateDescriptorSetLayout(mDevice, layoutInfo, null, pDescriptorSetLayout);

            if (res != VK_SUCCESS) {
                throw new RendererException(
                        String.format("Failed to create descriptor set layout! Res: %x", -res));
            }

            return pDescriptorSetLayout.get(0);
        }
    }

    /** Free the descriptor pool. */
    @Override
    public void free() {
        if (mDescriptorSetInfos != null) {
            for (DescriptorSetInfo info : mDescriptorSetInfos) {
                info.mUniformBuffer.free();
            }
        }
        mDescriptorSetInfos = null;

        vkDestroyDescriptorPool(mDevice, mPool, null);
        vkDestroyDescriptorSetLayout(mDevice, mSetLayout, null);

        mDescriptorSets = null;
    }
}
