/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

import java.nio.LongBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Resource;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

/**
 * Allows to load shaders for GPU.
 *
 * @author Aurimas Blažulionis
 *     <p>TODO: Turn this into a factory?
 */
@Log
public class Shader implements NativeResource {
    /** Vulkan device that the shader is loaded on. */
    private VkDevice mDevice;

    /** Handle to the loaded shader module. */
    @Accessors(prefix = "m")
    @Getter
    private long mModule;

    /**
     * Load the shader on GPU and get its handle.
     *
     * @param shader shader buffer to load.
     * @param device logical vulkan device to load the shader on.
     * @return handle to the on-GPU shader module. {@code null} if it fails to load.
     */
    public static Shader getShader(ShaderBuf shader, VkDevice device) {
        try (MemoryStack stack = stackPush()) {
            Shader ret = new Shader();
            ret.mDevice = device;

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(shader.getBuffer());

            LongBuffer pShaderModule = stack.longs(0);

            if (vkCreateShaderModule(device, createInfo, null, pShaderModule) == VK_SUCCESS) {
                ret.mModule = pShaderModule.get(0);
                return ret;
            } else {
                return null;
            }
        }
    }

    /**
     * Compile the shader, load it on GPU and get its handle.
     *
     * @param name name of the shader.
     * @param kind kind of the shader.
     * @param device logical vulkan device to load the shader on.
     * @return handle to the on-GPU shader module. {@code null} if it fails to load.
     */
    public static Shader getShader(String name, ShaderKind kind, VkDevice device) {
        log.fine("Get shader... " + name);

        try (Resource<ShaderBuf> resource = ShaderBuf.getResource(name, kind)) {
            return resource == null ? null : getShader(resource.get(), device);
        }
    }

    @Override
    public final void free() {
        vkDestroyShaderModule(mDevice, mModule, null);
    }
}
