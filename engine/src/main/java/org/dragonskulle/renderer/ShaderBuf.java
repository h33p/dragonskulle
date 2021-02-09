/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

import java.nio.ByteBuffer;
import lombok.Getter;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

public class ShaderBuf implements NativeResource {

    private long handle;
    @Getter private ByteBuffer buffer;

    public static Resource<ShaderBuf> getResource(String name) {
        return ResourceManager.getResource(
                ShaderBuf.class,
                (buffer) -> {
                    ShaderBuf ret = new ShaderBuf();
                    ret.buffer = MemoryUtil.memAlloc(buffer.length);
                    ret.buffer.put(buffer);
                    ret.buffer.rewind();
                    return ret;
                },
                name);
    }

    public static ShaderBuf compileShader(String name, ShaderKind shaderKind) {
        RenderedApp.LOGGER.info("Compiling " + name);

        String data = ResourceManager.loadResource(String::new, name);

        if (data == null) {
            RenderedApp.LOGGER.warning("Failed to find resource named: " + name);
            return null;
        }

        long compiler = shaderc_compiler_initialize();

        if (compiler == NULL) {
            RenderedApp.LOGGER.warning("Failed to create shader compiler!");
            return null;
        }

        long result =
                shaderc_compile_into_spv(compiler, data, shaderKind.getKind(), name, "main", NULL);

        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            RenderedApp.LOGGER.warning(
                    "Failed to compile shader "
                            + name
                            + ": "
                            + shaderc_result_get_error_message(result));
            return null;
        }

        shaderc_compiler_release(compiler);

        ShaderBuf ret = new ShaderBuf();
        ret.handle = result;
        ret.buffer = shaderc_result_get_bytes(result);

        return ret;
    }

    @Override
    public final void free() {
        if (handle != NULL) {
            shaderc_result_release(handle);
            handle = NULL;
        }
    }
}
