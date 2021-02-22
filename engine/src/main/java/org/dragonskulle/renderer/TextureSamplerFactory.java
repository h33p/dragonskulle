/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.HashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkDevice;

class TextureSamplerFactory {

    VkDevice device;
    float maxAnisotropy;
    boolean anisotropyEnable;
    HashMap<TextureMapping, Long> samplers;

    public TextureSamplerFactory(VkDevice device, PhysicalDevice physicalDevice) {
        this.device = device;
        anisotropyEnable = physicalDevice.featureSupport.anisotropyEnable;
        maxAnisotropy = physicalDevice.featureSupport.maxAnisotropy;
        samplers = new HashMap<>();
    }

    public long getSampler(TextureMapping mapping) {

        Long sampler = samplers.get(mapping);
        if (sampler == null) {
            sampler = createSampler(mapping, anisotropyEnable);
            samplers.put(mapping, sampler);
        }
        return sampler;
    }

    public void free() {
        samplers.values().stream().forEach(d -> vkDestroySampler(device, d, null));
    }

    private long createSampler(TextureMapping mapping, boolean anisotropyEnable) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.callocStack(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(mapping.filtering.getValue());
            samplerInfo.minFilter(mapping.filtering.getValue());
            samplerInfo.addressModeU(mapping.wrap_u.getValue());
            samplerInfo.addressModeV(mapping.wrap_v.getValue());
            samplerInfo.addressModeW(mapping.wrap_w.getValue());
            samplerInfo.anisotropyEnable(anisotropyEnable);
            samplerInfo.maxAnisotropy(maxAnisotropy);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_TRANSPARENT_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            samplerInfo.mipLodBias(0.0f);
            samplerInfo.minLod(0.0f);
            samplerInfo.maxLod(0.0f);

            LongBuffer pSampler = stack.longs(0);

            int res = vkCreateSampler(device, samplerInfo, null, pSampler);

            if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to setup sampler! Ret: %x", -res));
            }

            return pSampler.get(0);
        }
    }
}
