/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
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
public class UnlitMaterial implements IMaterial, Serializable {
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

    /** Constructor for UnlitMaterial */
    public UnlitMaterial() {}

    /**
     * Constructor for UnlitMaterial
     *
     * @param texture initial texture of the object
     */
    public UnlitMaterial(SampledTexture texture) {
        mFragmentTextures[0] = texture;
    }

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