/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_geometry_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Describes the kind of shader
 *
 * @author Aurimas Blažulionis
 */
public enum ShaderKind {
    VERTEX_SHADER(shaderc_glsl_vertex_shader),
    GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
    FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

    /** Numerical value of the shader */
    @Accessors(prefix = "m")
    @Getter
    private final int mKind;

    ShaderKind(int kind) {
        this.mKind = kind;
    }

    @Override
    public String toString() {
        if (this == VERTEX_SHADER) {
            return "vert";
        } else if (this == GEOMETRY_SHADER) {
            return "geom";
        } else if (this == FRAGMENT_SHADER) {
            return "frag";
        } else {
            return "none";
        }
    }
}
