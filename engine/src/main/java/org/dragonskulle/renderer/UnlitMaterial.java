/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import org.dragonskulle.renderer.TextureMapping.*;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * Reference unlit material
 *
 * <p>This material provides basic unlit rendering of objects with textures. One texture is
 * settable, alongside per-object colour value.
 *
 * @author Aurimas Blažulionis
 */
public class UnlitMaterial implements IMaterial {
    public static class UnlitShaderSet extends ShaderSet {
        public UnlitShaderSet() {
            mVertexShader = ShaderBuf.getResource("unlit", ShaderKind.VERTEX_SHADER);
            mFragmentShader = ShaderBuf.getResource("unlit", ShaderKind.FRAGMENT_SHADER);

            mVertexBindingDescription = BindingDescription.instancedWithMatrix(4 * 3);
            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            new AttributeDescription(1, 0, VK_FORMAT_R32G32B32_SFLOAT, 0));
            mNumFragmentTextures = 1;
        }
    }

    private static UnlitShaderSet sShaderSet = new UnlitShaderSet();

    private SampledTexture[] mFragmentTextures = {
        new SampledTexture(
                Texture.getResource("test_cc0_texture.jpg"),
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT))
    };

    /** Colour of the surface. It will multiply the texture's colour */
    public Vector3f colour = new Vector3f(1.f);

    public ShaderSet getShaderSet() {
        return sShaderSet;
    }

    public void writeVertexInstanceData(int offset, ByteBuffer buffer, Matrix4fc matrix) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        colour.get(offset, buffer);
    }

    public SampledTexture[] getFragmentTextures() {
        return mFragmentTextures;
    }

    public void free() {
        for (SampledTexture tex : mFragmentTextures) tex.free();
    }
}
