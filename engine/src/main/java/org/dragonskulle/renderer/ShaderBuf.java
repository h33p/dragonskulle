/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
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

    @Accessors(prefix = "m")
    @Getter
    public static class MacroDefinition {
        private final String mName;
        private final String mValue;

        public MacroDefinition(String name, String value) {
            mName = name;
            mValue = value;
        }
    }

    @EqualsAndHashCode
    private static class ShaderBufLoadArgs {
        private final ShaderKind mKind;
        private final MacroDefinition[] mDefinitions;

        ShaderBufLoadArgs(ShaderKind kind, MacroDefinition... definitions) {
            mKind = kind;
            mDefinitions = definitions;
        }
    }

    static {
        ResourceManager.registerResource(
                ShaderBuf.class,
                ShaderBufLoadArgs.class,
                (args) ->
                        String.format(
                                "shaders/%s.%s",
                                args.getName(), args.getAdditionalArgs().mKind.toString()),
                (buffer, args) ->
                        compileShader(
                                new String(buffer), args.getName(), args.getAdditionalArgs()));
    }

    /**
     * Load a shader resource
     *
     * @param name name of the shader
     * @param kind kind of the shader (vertex, fragment, geometry)
     * @return shader resource if loaded, null otherwise
     */
    public static Resource<ShaderBuf> getResource(String name, ShaderKind kind) {
        return ResourceManager.getResource(ShaderBuf.class, name, new ShaderBufLoadArgs(kind));
    }

    /**
     * Compile a shader directly
     *
     * @param name name of the shader
     * @param data shader bytecode
     * @param shaderKind shader kind (vertex, fragment, geometry)
     * @return compiled shader, null if there was an error
     */
    public static ShaderBuf compileShader(String data, String name, ShaderBufLoadArgs args) {
        ShaderKind shaderKind = args.mKind;
        MacroDefinition[] macros = args.mDefinitions;

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

        for (MacroDefinition macro : macros) {
            shaderc_compile_options_add_macro_definition(options, macro.mName, macro.mValue);
        }

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
