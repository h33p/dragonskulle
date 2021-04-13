/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.NativeResource;

/**
 * Describes a raw SPIR-V shader buffer
 *
 * @author Aurimas Blažulionis
 */
@Log
public class ShaderBuf implements NativeResource {
    /** Handle to compiled SPIR-V shader */
    private long mHandle;

    /** Byte view to the shader */
    @Accessors(prefix = "m")
    @Getter
    private ByteBuffer mBuffer;

    /** macro key value pairs */
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

    /** Arguments that can be set when loading {@link ShaderBuf}s */
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
                                new String(buffer),
                                args.getName(),
                                args.getAdditionalArgs().mKind,
                                args.getAdditionalArgs().mDefinitions));
    }

    /**
     * Load a shader resource
     *
     * @param name name of the shader
     * @param kind kind of the shader (vertex, fragment, geometry)
     * @return shader resource if loaded, null otherwise
     */
    public static Resource<ShaderBuf> getResource(
            String name, ShaderKind kind, MacroDefinition... macros) {
        return ResourceManager.getResource(
                ShaderBuf.class, name, new ShaderBufLoadArgs(kind, macros));
    }

    /**
     * Processes #include directives and produces final output
     *
     * @param data text data to process
     * @param depth current depth of the file. A hard limit is imposed (currently 20) of how deep
     *     the file is recursed before ignoring the directives
     * @return preprocessed text data
     */
    private static String processIncludes(String data, int depth) {
        if (depth >= 20) return data;

        String lines[] = data.split("\\r?\\n");

        // https://stackoverflow.com/a/26493311/13240247
        Pattern pat = Pattern.compile("\\s*#include\\s*([<\"])([^>\"]+)([>\"])");

        for (int i = 0; i < lines.length; i++) {
            Matcher match = pat.matcher(lines[i]);
            if (match.find()) {
                // Second group contains the file
                String m = match.group(2);

                try (Resource<String> res =
                        ResourceManager.getResource(String.class, "shaders/" + m)) {
                    if (res == null) {
                        continue;
                    }

                    lines[i] = processIncludes(res.get(), depth + 1);
                }
            }
        }

        return String.join("\n", lines);
    }

    /**
     * Compile a shader directly
     *
     * @param data shader bytecode
     * @param name name of the shader
     * @param shaderKind shader kind (vertex, fragment, geometry)
     * @param macros custom macro definitions
     * @return compiled shader, null if there was an error
     */
    public static ShaderBuf compileShader(
            String data, String name, ShaderKind shaderKind, MacroDefinition[] macros) {
        log.fine("Compiling " + name);

        if (data == null) {
            log.warning("Failed to find resource named: " + name);
            return null;
        }

        data = processIncludes(data, 0);

        long compiler = shaderc_compiler_initialize();

        if (compiler == NULL) {
            log.warning("Failed to create shader compiler!");
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
            log.warning(
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
