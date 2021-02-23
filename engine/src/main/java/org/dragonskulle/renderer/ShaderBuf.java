/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

/**
 * Describes a raw SPIR-V shader buffer
 *
 * @author Aurimas Blažulionis
 */
public class ShaderBuf implements NativeResource {
    private long mHandle;

    @Accessors(prefix = "m")
    @Getter
    private ByteBuffer mBuffer;

    /** Load a shader resource */
    public static Resource<ShaderBuf> getResource(String name, ShaderKind kind) {
        String spirvName = String.format("shaderc/%s.%s.spv", name, kind.toString());

        Resource<ShaderBuf> precompiledShader =
                ResourceManager.getResource(
                        ShaderBuf.class,
                        (buffer) -> {
                            ShaderBuf ret = new ShaderBuf();
                            ret.mBuffer = MemoryUtil.memAlloc(buffer.length);
                            ret.mBuffer.put(buffer);
                            ret.mBuffer.rewind();
                            return ret;
                        },
                        spirvName);

        if (precompiledShader != null) return precompiledShader;

        String glslName = String.format("shaders/%s.%s", name, kind.toString());

        return ResourceManager.getResource(
                ShaderBuf.class,
                (buffer) -> compileShader(glslName, new String(buffer), kind),
                glslName);
    }

    /** Compile a shader directly */
    public static ShaderBuf compileShader(String name, String data, ShaderKind shaderKind) {
        RenderedApp.LOGGER.info("Compiling " + name);

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
        ret.mHandle = result;
        ret.mBuffer = shaderc_result_get_bytes(result);

        return ret;
    }

    @Override
    public final void free() {
        if (mHandle != NULL) {
            shaderc_result_release(mHandle);
            mHandle = NULL;
        }
        mBuffer = null;
    }
}
