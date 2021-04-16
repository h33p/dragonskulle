/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;

import java.io.Serializable;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Describe the way texture is mapped on surfaces.
 *
 * @author Aurimas Blažulionis
 */
public class TextureMapping implements Serializable {

    /** Controls whether the texture uses linear or nearest filtering. */
    public TextureFiltering mFiltering;

    /** How the texture will wrap on the U coordinate. */
    public TextureWrapping mWrapU;
    /** How the texture will wrap on the V coordinate. */
    public TextureWrapping mWrapV;
    /** How the texture will wrap on the W coordinate. */
    public TextureWrapping mWrapW;

    /**
     * Constructor for {@link TextureMapping}.
     *
     * @param filtering filtering mode to use
     * @param wrapU U coordinate wrapping
     * @param wrapV V coordinate wrapping
     * @param wrapW W coordinate wrapping
     */
    public TextureMapping(
            TextureFiltering filtering,
            TextureWrapping wrapU,
            TextureWrapping wrapV,
            TextureWrapping wrapW) {
        this.mFiltering = filtering;
        this.mWrapU = wrapU;
        this.mWrapV = wrapV;
        this.mWrapW = wrapW;
    }

    /**
     * Constructor for {@link TextureMapping}.
     *
     * @param filtering filtering mode to use
     * @param wrap wrapping mode for all texture coordinates
     */
    public TextureMapping(TextureFiltering filtering, TextureWrapping wrap) {
        this(filtering, wrap, wrap, wrap);
    }

    /** Default constructor for {@link TextureMapping}. */
    public TextureMapping() {
        this(TextureFiltering.LINEAR, TextureWrapping.CLAMP);
    }

    /** Describes texture filtering. */
    @Accessors(prefix = "m")
    public static enum TextureFiltering {
        /** Nearest filtering looks pixelated. */
        NEAREST(VK_FILTER_NEAREST),
        /** Linear filtering is smooth. */
        LINEAR(VK_FILTER_LINEAR);

        @Getter private final int mValue;

        private TextureFiltering(int value) {
            this.mValue = value;
        }

        /** Create texture filtering from glTF filtering values. */
        public static TextureFiltering fromGLTF(Integer value) {
            if (value == null) {
                return LINEAR;
            }

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

    /** Describes texture wrapping. */
    @Accessors(prefix = "m")
    public static enum TextureWrapping {
        /** Repeat the texture. */
        REPEAT(VK_SAMPLER_ADDRESS_MODE_REPEAT),
        /** Repeat and mirror the texture. */
        MIRROR(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT),
        /** Clamp the pixels to their edge values. */
        CLAMP(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE),
        /** Clamp the texture to a solid border colour. */
        CLAMP_BORDER(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER);

        @Getter private final int mValue;

        private TextureWrapping(int value) {
            this.mValue = value;
        }

        /** Create a {@link TextureWrapping} from glTF wrapping value. */
        public static TextureWrapping fromGLTF(Integer value) {
            if (value == null) {
                return REPEAT;
            }

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
        return mFiltering.mValue + mWrapU.mValue * 10 + mWrapV.mValue * 100 + mWrapW.mValue * 1000;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TextureMapping)) {
            return false;
        }
        return hashCode() == o.hashCode();
    }
}
