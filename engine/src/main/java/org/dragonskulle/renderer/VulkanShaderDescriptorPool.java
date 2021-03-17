/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

/**
 * Class abstracting a vulkan descriptor pool and their sets for a single shader set
 *
 * <p>Normally, one pool is created per material/pipeline, unless multiple passes are rendered.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class VulkanShaderDescriptorPool implements NativeResource {

    @Builder
    private static class DescriptorBinding {
        public int bindingID;
        public int size;
    }

    @Builder
    private static class DescriptorSetInfo {
        DescriptorBinding bindingDescription;
        VulkanBuffer uniformBuffer;
        int stageFlags;
    }

    private VkDevice mDevice;
    private long mPool;
    @Getter private long mSetLayout;
    private long[] mDescriptorSets;

    private DescriptorSetInfo[] mDescriptorSetInfos;

    public static final Logger LOGGER = Logger.getLogger("render");

    public static VulkanShaderDescriptorPool createPool(
            VkDevice device,
            PhysicalDevice physicalDevice,
            ShaderSet shaderSet,
            int descriptorCount) {
        VulkanShaderDescriptorPool ret = new VulkanShaderDescriptorPool();
        ret.mDevice = device;
        ret.mPool = ret.createDescriptorPool(shaderSet, descriptorCount);

        if (ret.mPool == 0) return null;

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

            if (res != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create descriptor sets! Res: %x", -res));

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
                bufferInfo.offset(info.bindingDescription.size * index);
                bufferInfo.range(info.bindingDescription.size);
                bufferInfo.buffer(info.uniformBuffer.buffer);

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                uboDescriptorWrite.dstBinding(info.bindingDescription.bindingID);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfo);
                uboDescriptorWrite.dstSet(descriptorSet);
            }

            vkUpdateDescriptorSets(mDevice, descriptorWrites, null);
        }
    }

    private long createDescriptorPool(ShaderSet shaderSet, int descriptorCount) {
        LOGGER.fine("Setup descriptor pool");

        try (MemoryStack stack = stackPush()) {

            int layoutCount = shaderSet.numUniformBindings();

            if (layoutCount == 0) return 0;

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
            ShaderSet shaderSet, PhysicalDevice physicalDevice, int descriptorCount) {
        LOGGER.fine("Create descriptor set layout");

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
                if (dataInfo[0] < 0) continue;

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
                uboLayoutBinding.stageFlags(info.stageFlags);
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

        if (mDescriptorSetInfos != null)
            for (DescriptorSetInfo info : mDescriptorSetInfos) info.uniformBuffer.free();
        mDescriptorSetInfos = null;

        vkDestroyDescriptorPool(mDevice, mPool, null);
        vkDestroyDescriptorSetLayout(mDevice, mSetLayout, null);

        mDescriptorSets = null;
    }
}
