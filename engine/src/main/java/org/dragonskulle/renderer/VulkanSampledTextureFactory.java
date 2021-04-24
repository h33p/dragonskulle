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

        private Entry(VulkanSampledTextureFactory factory, SampledTexture texture) {
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
            mSampledTexture =
                    new VulkanSampledTexture(
                            mImage.createImageView(),
                            factory.mSamplerFactory.getSampler(
                                    texture.getMapping(), mImage.getMipLevels()));
        }
    }

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

    public VulkanSampledTexture getTexture(SampledTexture texture) {
        Entry entry = mTextures.computeIfAbsent(texture, k -> new Entry(this, texture));
        return entry.mSampledTexture;
    }

    @Override
    public void free() {
        for (Entry entry : mTextures.values()) {
            vkDestroyImageView(mDevice, entry.mSampledTexture.getImageView(), null);
            entry.mImage.free();
        }
    }
}
