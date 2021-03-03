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
class TextureSetLayoutFactory implements NativeResource {
    private VkDevice mDevice;

    private HashMap<Integer, Long> mLayouts = new HashMap<>();

    public TextureSetLayoutFactory(VkDevice device) {
        mDevice = device;
    }

    public Long getLayout(int textureCount) {

        if (textureCount < 0) return null;

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
     */
    private long createDescriptorSetLayout(int textureCount) {
        Renderer.LOGGER.info("Create texture descriptor set layout");

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
                throw new RuntimeException(
                        String.format("Failed to create descriptor set layout! Res: %x", -res));
            }

            return pDescriptorSetLayout.get(0);
        }
    }

    public void free() {
        for (long layout : mLayouts.values()) {
            vkDestroyDescriptorSetLayout(mDevice, layout, null);
        }
        mLayouts.clear();
    }
}
