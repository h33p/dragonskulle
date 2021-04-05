/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import lombok.Getter;

/**
 * Describe the way texture is mapped on surfaces
 *
 * @author Aurimas Blažulionis
 */
public class TextureMapping implements Serializable {

    public TextureFiltering filtering;

    public TextureWrapping wrapU;
    public TextureWrapping wrapV;
    public TextureWrapping wrapW;

    public TextureMapping(
            TextureFiltering filtering,
            TextureWrapping wrapU,
            TextureWrapping wrapV,
            TextureWrapping wrapW) {
        this.filtering = filtering;
        this.wrapU = wrapU;
        this.wrapV = wrapV;
        this.wrapW = wrapW;
    }

    public TextureMapping(TextureFiltering filtering, TextureWrapping wrap) {
        this(filtering, wrap, wrap, wrap);
    }

    public TextureMapping() {
        this(TextureFiltering.LINEAR, TextureWrapping.CLAMP);
    }

    public static enum TextureFiltering {
        NEAREST(VK_FILTER_NEAREST),
        LINEAR(VK_FILTER_LINEAR);

        @Getter private final int value;

        private TextureFiltering(int value) {
            this.value = value;
        }

        public static TextureFiltering fromGLTF(Integer value) {
            if (value == null) return LINEAR;

            switch (value) {
                case 9728: // NEAREST
                case 9986: // NEAREST_MIPMAP_LINEAR
                case 9984: // NEAREST_MIPMAP_NEAREST
                    return NEAREST;
                default:
                    return LINEAR;
            }
        }
    }

    public static enum TextureWrapping {
        REPEAT(VK_SAMPLER_ADDRESS_MODE_REPEAT),
        MIRROR(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT),
        CLAMP(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE),
        CLAMP_BORDER(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER);

        @Getter private final int value;

        private TextureWrapping(int value) {
            this.value = value;
        }

        public static TextureWrapping fromGLTF(Integer value) {
            if (value == null) return REPEAT;

            // There is no CLAMP_BORDER value
            switch (value) {
                case 33648: // MIRRORED_REPEAT
                    return MIRROR;
                case 10497: // REPEAT
                    return REPEAT;
                default:
                    return CLAMP;
            }
        }
    }

    @Override
    public int hashCode() {
        return filtering.value + wrapU.value * 10 + wrapV.value * 100 + wrapW.value * 1000;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TextureMapping)) return false;
        return hashCode() == o.hashCode();
    }
}
