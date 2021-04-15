/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkDevice;

/**
 * Create and manage texture samplers for a device
 *
 * @author Aurimas Blažulionis
 */
class TextureSamplerFactory implements NativeResource {
    /** Underlying vulkan device */
    private VkDevice mDevice;
    /** Highest anisotrophic filtering value */
    private float mMaxAnisotropy;
    /** Controls whether anisotrophic filtering is enabled */
    private boolean mAnisotropyEnable;
    /** Stored sampler handles */
    private HashMap<SamplerDescriptor, Long> mSamplers;

    /** Describes a sampler */
    @EqualsAndHashCode
    private static class SamplerDescriptor {
        /** Texture mapping used */
        TextureMapping mMapping;
        /** Number of mip map levels used */
        int mMipLevels;

        public SamplerDescriptor(TextureMapping mapping, int mipLevels) {
            mMapping = mapping;
            mMipLevels = mipLevels;
        }
    }

    /**
     * Constructor for {@link TextureSamplerFactory}
     *
     * @param device vulkan logical device to use
     * @param physicalDevice vulkan physical device to use (must be the same one for {@code device})
     */
    public TextureSamplerFactory(VkDevice device, PhysicalDevice physicalDevice) {
        mDevice = device;
        mAnisotropyEnable = physicalDevice.getFeatureSupport().anisotropyEnable;
        mMaxAnisotropy = physicalDevice.getFeatureSupport().maxAnisotropy;
        mSamplers = new HashMap<>();
    }

    /**
     * Get a sampler with specified texture mapping
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

    /**
     * Create a sampler
     *
     * @param desc sampler description
     * @param anisotropyEnable controls whether anisotrophic filtering is enabled
     * @return handle to the sampler
     */
    private long createSampler(SamplerDescriptor desc, boolean anisotropyEnable) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.callocStack(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(desc.mMapping.filtering.getValue());
            samplerInfo.minFilter(desc.mMapping.filtering.getValue());
            samplerInfo.addressModeU(desc.mMapping.wrapU.getValue());
            samplerInfo.addressModeV(desc.mMapping.wrapV.getValue());
            samplerInfo.addressModeW(desc.mMapping.wrapW.getValue());
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
