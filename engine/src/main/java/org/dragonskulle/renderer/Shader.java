/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import lombok.Getter;
import org.dragonskulle.core.Resource;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

public class Shader implements NativeResource {

    private VkDevice device;
    @Getter private long module;

    public static Shader getShader(String name, ShaderKind kind, VkDevice device) {
        RenderedApp.LOGGER.fine("Get shader... " + name);

        try (Resource<ShaderBuf> resource = ShaderBuf.getResource(name, kind)) {
            if (resource == null) return null;

            try (MemoryStack stack = stackPush()) {
                Shader ret = new Shader();
                ret.device = device;

                VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
                createInfo.pCode(resource.get().getBuffer());

                LongBuffer pShaderModule = stack.longs(0);

                if (vkCreateShaderModule(device, createInfo, null, pShaderModule) == VK_SUCCESS) {
                    ret.module = pShaderModule.get(0);
                    return ret;
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public final void free() {
        vkDestroyShaderModule(device, module, null);
    }
}
