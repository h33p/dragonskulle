/* (C) 2021 DragonSkulle */

package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BORDER_COLOR_INT_TRANSPARENT_BLACK;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import java.nio.LongBuffer;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

/**
 * Create and manage texture samplers for a device.
 *
 * @author Aurimas Blažulionis
 */
class TextureSamplerFactory implements NativeResource {
    private VkDevice mDevice;
    private float mMaxAnisotropy;
    private boolean mAnisotropyEnable;
    private HashMap<SamplerDescriptor, Long> mSamplers;

    @EqualsAndHashCode
    private static class SamplerDescriptor {
        TextureMapping mMapping;
        int mMipLevels;

        public SamplerDescriptor(TextureMapping mapping, int mipLevels) {
            mMapping = mapping;
            mMipLevels = mipLevels;
        }
    }

    public TextureSamplerFactory(VkDevice device, PhysicalDevice physicalDevice) {
        mDevice = device;
        mAnisotropyEnable = physicalDevice.getFeatureSupport().anisotropyEnable;
        mMaxAnisotropy = physicalDevice.getFeatureSupport().maxAnisotropy;
        mSamplers = new HashMap<>();
    }

    /**
     * Get a sampler with specified texture mapping.
     *
     * @param mapping texture mapping properties to get the sampler for
     * @param mipLevels mip map levels to use
     */
    public long getSampler(TextureMapping mapping, int mipLevels) {
        SamplerDescriptor desc = new SamplerDescriptor(mapping, mipLevels);
        Long sampler = mSamplers.get(desc);
        if (sampler == null) {
            sampler = createSampler(desc, mAnisotropyEnable);
            mSamplers.put(desc, sampler);
        }
        return sampler;
    }

    @Override
    public void free() {
        mSamplers.values().stream().forEach(d -> vkDestroySampler(mDevice, d, null));
    }

    private long createSampler(SamplerDescriptor desc, boolean anisotropyEnable) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.callocStack(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(desc.mMapping.mFiltering.getmValue());
            samplerInfo.minFilter(desc.mMapping.mFiltering.getmValue());
            samplerInfo.addressModeU(desc.mMapping.mWrapU.getValue());
            samplerInfo.addressModeV(desc.mMapping.mWrapV.getValue());
            samplerInfo.addressModeW(desc.mMapping.mWrapW.getValue());
            samplerInfo.anisotropyEnable(anisotropyEnable);
            samplerInfo.maxAnisotropy(mMaxAnisotropy);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_TRANSPARENT_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            samplerInfo.mipLodBias(0.0f);
            samplerInfo.minLod(0.0f);
            samplerInfo.maxLod(desc.mMipLevels);

            LongBuffer pSampler = stack.longs(0);

            int res = vkCreateSampler(mDevice, samplerInfo, null, pSampler);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to setup sampler! Ret: %x", -res));
            }

            return pSampler.get(0);
        }
    }
}
