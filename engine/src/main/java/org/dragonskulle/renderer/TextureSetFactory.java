/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.util.Arrays;
import java.util.HashMap;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDevice;

/**
 * Create and manage texture samplers for a device.
 *
 * @author Aurimas Blažulionis
 */
class TextureSetFactory implements NativeResource {
    /** Logical device to use. */
    private VkDevice mDevice;
    /**
     * Number of descriptor sets. This value comes from renderer's image count - the number of
     * swapchain images used.
     */
    private int mDescriptorSetCount;

    /** Factory for texture set layouts. */
    private TextureSetLayoutFactory mLayoutFactory;

    /** Maps to cached texture sets. */
    private HashMap<Integer, TextureSet> mTextureSets = new HashMap<>();

    /**
     * Construct a {@link TextureSetFactory}.
     *
     * @param device target logical device to use.
     * @param layoutFactory texture set layout factory.
     * @param descriptorSetCount number of descriptor sets allocated. Comes from renderer's number
     *     of swapchain images.
     */
    public TextureSetFactory(
            VkDevice device, TextureSetLayoutFactory layoutFactory, int descriptorSetCount) {
        mDevice = device;
        mDescriptorSetCount = descriptorSetCount;
        mLayoutFactory = layoutFactory;
    }

    /**
     * Get a combined texture descriptor set.
     *
     * @param textures sampled GPU textures to combine into a set.
     * @return the combined texture set.
     * @throws RendererException if there is a failure creating the texture set.
     */
    public TextureSet getSet(VulkanSampledTexture[] textures) throws RendererException {
        if (textures.length == 0) {
            return null;
        }

        Integer hash = Arrays.hashCode(textures);
        TextureSet set = mTextureSets.get(hash);

        if (set == null) {
            set = new TextureSet(mDevice, mLayoutFactory, textures, mDescriptorSetCount);
            mTextureSets.put(hash, set);
        }

        return set;
    }

    /**
     * Get a combined texture descriptor set.
     *
     * @param textures sampled textures to combine into a set.
     * @param factory sampled texture factory to use.
     * @return the combined texture set.
     * @throws RendererException if there is a image allocation failure.
     */
    public TextureSet getSet(SampledTexture[] textures, VulkanSampledTextureFactory factory)
            throws RendererException {
        VulkanSampledTexture[] vulkanTextures = new VulkanSampledTexture[textures.length];
        for (int i = 0; i < textures.length; i++) {
            vulkanTextures[i] = factory.getTexture(textures[i]);
        }
        return getSet(vulkanTextures);
    }

    /** Free all texture sets. */
    @Override
    public void free() {
        for (TextureSet set : mTextureSets.values()) {
            set.free();
        }
        mTextureSets.clear();
    }
}
