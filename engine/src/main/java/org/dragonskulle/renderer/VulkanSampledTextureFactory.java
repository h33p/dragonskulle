/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.vkDestroyImageView;

import java.util.HashMap;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

/**
 * Class abstracting a single render instance.
 *
 * <p>This stores all properties that can differ between different object instances
 *
 * @author Aurimas Blažulionis
 */
class VulkanSampledTextureFactory implements NativeResource {
    private VkDevice mDevice;
    private PhysicalDevice mPhysicalDevice;
    private long mCommandPool;
    private VkQueue mGraphicsQueue;
    private TextureSamplerFactory mSamplerFactory;
    private HashMap<SampledTexture, Entry> mTextures = new HashMap<>();

    private static class Entry {
        VulkanSampledTexture mSampledTexture;
        VulkanImage mImage;

        /**
         * Create a sampled texture entry, loading the texture onto GPU memory.
         *
         * @param factory one-time parent sampled texture factory reference.
         * @param texture texture to crate the entry for.
         * @throws RendererException if loading the image fails.
         */
        private Entry(VulkanSampledTextureFactory factory, SampledTexture texture)
                throws RendererException {
            VkCommandBuffer cmd =
                    Renderer.beginSingleUseCommandBuffer(factory.mDevice, factory.mCommandPool);
            mImage =
                    new VulkanImage(
                            texture.getTexture().get(),
                            texture.isLinear(),
                            cmd,
                            factory.mDevice,
                            factory.mPhysicalDevice);
            Renderer.endSingleUseCommandBuffer(
                    cmd, factory.mDevice, factory.mGraphicsQueue, factory.mCommandPool);
            mImage.freeStagingBuffer();
            mSampledTexture =
                    new VulkanSampledTexture(
                            mImage.createImageView(),
                            factory.mSamplerFactory.getSampler(
                                    texture.getMapping(), mImage.getMipLevels()));
        }
    }

    /**
     * Create a sampled texture factory.
     *
     * @param device logical device to use.
     * @param physicalDevice physical device to use.
     * @param commandPool command pool for queueing operations.
     * @param graphicsQueue graphics queue to use for command buffer operations.
     * @param samplerFactory texture sampler factory.
     */
    public VulkanSampledTextureFactory(
            VkDevice device,
            PhysicalDevice physicalDevice,
            long commandPool,
            VkQueue graphicsQueue,
            TextureSamplerFactory samplerFactory) {
        mDevice = device;
        mPhysicalDevice = physicalDevice;
        mCommandPool = commandPool;
        mGraphicsQueue = graphicsQueue;
        mSamplerFactory = samplerFactory;
    }

    /**
     * Get a vulkan texture from input texture. Load it on GPU if needed.
     *
     * @param texture target texture to retrieve.
     * @return On-GPU texture.
     * @throws RendererException if loading the texture fails.
     */
    public VulkanSampledTexture getTexture(SampledTexture texture) throws RendererException {
        Entry entry = mTextures.get(texture);

        if (entry == null) {
            entry = new Entry(this, texture);
            mTextures.put(texture, entry);
        }

        return entry.mSampledTexture;
    }

    /** Free all textures. */
    @Override
    public void free() {
        for (Entry entry : mTextures.values()) {
            vkDestroyImageView(mDevice, entry.mSampledTexture.getImageView(), null);
            entry.mImage.free();
        }
    }
}
