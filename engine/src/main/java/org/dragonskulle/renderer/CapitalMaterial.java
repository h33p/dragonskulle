/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.io.Serializable;
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
 * @author Oscar L
 */
public class CapitalMaterial implements IMaterial, Serializable {
    private static org.dragonskulle.renderer.UnlitMaterial.UnlitShaderSet sShaderSet =
            new org.dragonskulle.renderer.UnlitMaterial.UnlitShaderSet();

    private SampledTexture[] mFragmentTextures = {
        new SampledTexture(
                Texture.getResource("cat_material.jpg"),
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
