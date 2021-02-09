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

        String spirvName = String.format("shaderc/%s.%s.spv", name, kind.toString());
        String glslName = String.format("shaders/%s.%s", name, kind.toString());

        try (Resource<ShaderBuf> res = ShaderBuf.getResource(spirvName)) {
            ShaderBuf resource = res != null ? res.get() : ShaderBuf.compileShader(glslName, kind);

            if (resource == null) return null;

            try (MemoryStack stack = stackPush()) {
                Shader ret = new Shader();
                ret.device = device;

                VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
                createInfo.pCode(resource.getBuffer());

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
