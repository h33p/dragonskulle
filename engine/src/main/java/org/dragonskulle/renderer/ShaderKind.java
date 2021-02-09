/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.util.shaderc.Shaderc.*;

import lombok.Getter;

public enum ShaderKind {
    VERTEX_SHADER(shaderc_glsl_vertex_shader),
    GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
    FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

    @Getter private final int kind;

    ShaderKind(int kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        if (this == VERTEX_SHADER) return "vert";
        else if (this == GEOMETRY_SHADER) return "geom";
        else if (this == FRAGMENT_SHADER) return "frag";
        else return "none";
    }
}
