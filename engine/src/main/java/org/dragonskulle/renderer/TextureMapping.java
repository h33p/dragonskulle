/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import lombok.Getter;

public class TextureMapping {

    public TextureFiltering filtering;

    public TextureWrapping wrap_u;
    public TextureWrapping wrap_v;
    public TextureWrapping wrap_w;

    public TextureMapping(
            TextureFiltering filtering,
            TextureWrapping wrap_u,
            TextureWrapping wrap_v,
            TextureWrapping wrap_w) {
        this.filtering = filtering;
        this.wrap_u = wrap_u;
        this.wrap_v = wrap_v;
        this.wrap_w = wrap_w;
    }

    public TextureMapping(TextureFiltering filtering, TextureWrapping wrap) {
        this(filtering, wrap, wrap, wrap);
    }

    public static enum TextureFiltering {
        NEAREST(VK_FILTER_NEAREST),
        LINEAR(VK_FILTER_LINEAR);

        @Getter private final int value;

        private TextureFiltering(int value) {
            this.value = value;
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
    }

    @Override
    public int hashCode() {
        return filtering.value + wrap_u.value * 10 + wrap_v.value * 100 + wrap_w.value * 1000;
    }
}
