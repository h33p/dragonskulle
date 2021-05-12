/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;

import java.nio.LongBuffer;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.java.Log;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

/**
 * Create and manage texture sampler layouts for a device
 *
 * <p>These layouts are freed whenever this factory is freed.
 *
 * @author Aurimas Blažulionis
 */
@Log
class TextureSetLayoutFactory implements NativeResource {
    /** Logical device use. */
    private VkDevice mDevice;

    /** Map from number of images and the texture set layouts. */
    private Map<Integer, Long> mLayouts = new TreeMap<>();

    /**
     * Constructor for {@link TextureSetLayoutFactory}.
     *
     * @param device logical device to use.
     */
    public TextureSetLayoutFactory(VkDevice device) {
        mDevice = device;
    }

    /**
     * Get a texture descriptor set layout
     *
     * <p>This layout does not need to be destroyed, it is cached within this factory.
     *
     * @param textureCount number of textures used in the layout
     * @return created layout, or null if textureCount was zero, or there was an error creating the
     *     layout
     * @throws RendererException if there is a failure creating the descriptor set layout.
     */
    public Long getLayout(int textureCount) throws RendererException {
        if (textureCount < 0) {
            return null;
        }

        Long layout = mLayouts.get(textureCount);

        if (layout == null) {
            layout = createDescriptorSetLayout(textureCount);
            mLayouts.put(textureCount, layout);
        }

        return layout;
    }

    /**
     * Create a descriptor set layout
     *
     * <p>This layout is used in creating descriptor sets. It describes all attached textures shader
     * has.
     *
     * @param textureCount number of textures this layout should have.
     * @return descriptor set layout for the number of images given.
     */
    private long createDescriptorSetLayout(int textureCount) throws RendererException {
        log.fine("Create texture descriptor set layout");

        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings =
                    VkDescriptorSetLayoutBinding.callocStack(textureCount, stack);

            for (int i = 0; i < textureCount; i++) {
                VkDescriptorSetLayoutBinding imageLayoutBinding = layoutBindings.get(i);
                imageLayoutBinding.binding(i);
                imageLayoutBinding.descriptorCount(1);
                imageLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                imageLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
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

    /** Free the underlying resources. */
    @Override
    public void free() {
        for (long layout : mLayouts.values()) {
            vkDestroyDescriptorSetLayout(mDevice, layout, null);
        }
        mLayouts.clear();
    }
}
