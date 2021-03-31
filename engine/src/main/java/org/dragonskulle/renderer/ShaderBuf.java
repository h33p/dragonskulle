/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger("render");

    /**
     * Load a shader resource
     *
     * @param name name of the shader
     * @param kind kind of the shader (vertex, fragment, geometry)
     * @return shader resource if loaded, null otherwise
     */
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

    /**
     * Compile a shader directly
     *
     * @param name name of the shader
     * @param data shader bytecode
     * @param shaderKind shader kind (vertex, fragment, geometry)
     * @return compiled shader, null if there was an error
     */
    public static ShaderBuf compileShader(String name, String data, ShaderKind shaderKind) {
        LOGGER.fine("Compiling " + name);

        if (data == null) {
            LOGGER.warning("Failed to find resource named: " + name);
            return null;
        }

        long compiler = shaderc_compiler_initialize();

        if (compiler == NULL) {
            LOGGER.warning("Failed to create shader compiler!");
            return null;
        }

        long options = shaderc_compile_options_initialize();

        shaderc_compile_options_set_optimization_level(
                options, shaderc_optimization_level_performance);

        long result =
                shaderc_compile_into_spv(
                        compiler, data, shaderKind.getKind(), name, "main", options);

        shaderc_compile_options_release(options);

        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            LOGGER.warning(
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
