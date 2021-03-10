/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.util.Arrays;
import java.util.HashMap;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDevice;

/**
 * Create and manage texture samplers for a device
 *
 * @author Aurimas Blažulionis
 */
class TextureSetFactory implements NativeResource {
    private VkDevice mDevice;
    private int mDescriptorSetCount;

    private TextureSetLayoutFactory mLayoutFactory;

    private HashMap<Integer, TextureSet> mTextureSets = new HashMap<>();

    public TextureSetFactory(
            VkDevice device, TextureSetLayoutFactory layoutFactory, int descriptorSetCount) {
        mDevice = device;
        mDescriptorSetCount = descriptorSetCount;
        mLayoutFactory = layoutFactory;
    }

    public TextureSet getSet(VulkanSampledTexture[] textures) {
        if (textures.length == 0) return null;

        Integer hash = Arrays.hashCode(textures);
        TextureSet set = mTextureSets.get(hash);

        if (set == null) {
            set = new TextureSet(mDevice, mLayoutFactory, textures, mDescriptorSetCount);
            mTextureSets.put(hash, set);
        }

        return set;
    }

    public TextureSet getSet(SampledTexture[] textures, VulkanSampledTextureFactory factory) {
        VulkanSampledTexture[] vulkanTextures = new VulkanSampledTexture[textures.length];
        for (int i = 0; i < textures.length; i++) {
            vulkanTextures[i] = factory.getTexture(textures[i]);
        }
        return getSet(vulkanTextures);
    }

    public void free() {
        for (TextureSet set : mTextureSets.values()) {
            set.free();
        }
        mTextureSets.clear();
    }
}
