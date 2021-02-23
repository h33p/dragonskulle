/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.HashMap;
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
    private VkDevice mDevice;
    private float mMaxAnisotropy;
    private boolean mAnisotropyEnable;
    private HashMap<TextureMapping, Long> mSamplers;

    public TextureSamplerFactory(VkDevice device, PhysicalDevice physicalDevice) {
        mDevice = device;
        mAnisotropyEnable = physicalDevice.featureSupport.anisotropyEnable;
        mMaxAnisotropy = physicalDevice.featureSupport.maxAnisotropy;
        mSamplers = new HashMap<>();
    }

    /**
     * Get a sampler with specified texture mapping
     *
     * @param mapping texture mapping properties to get the sampler for
     */
    public long getSampler(TextureMapping mapping) {
        Long sampler = mSamplers.get(mapping);
        if (sampler == null) {
            sampler = createSampler(mapping, mAnisotropyEnable);
            mSamplers.put(mapping, sampler);
        }
        return sampler;
    }

    @Override
    public void free() {
        mSamplers.values().stream().forEach(d -> vkDestroySampler(mDevice, d, null));
    }

    private long createSampler(TextureMapping mapping, boolean anisotropyEnable) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.callocStack(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(mapping.filtering.getValue());
            samplerInfo.minFilter(mapping.filtering.getValue());
            samplerInfo.addressModeU(mapping.wrapU.getValue());
            samplerInfo.addressModeV(mapping.wrapV.getValue());
            samplerInfo.addressModeW(mapping.wrapW.getValue());
            samplerInfo.anisotropyEnable(anisotropyEnable);
            samplerInfo.maxAnisotropy(mMaxAnisotropy);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_TRANSPARENT_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            samplerInfo.mipLodBias(0.0f);
            samplerInfo.minLod(0.0f);
            samplerInfo.maxLod(0.0f);

            LongBuffer pSampler = stack.longs(0);

            int res = vkCreateSampler(mDevice, samplerInfo, null, pSampler);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to setup sampler! Ret: %x", -res));
            }

            return pSampler.get(0);
        }
    }
}
