/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.IMaterial;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.*;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Reference unlit material
 *
 * <p>This material provides basic unlit rendering of objects with textures. One texture is
 * settable, alongside per-object colour value.
 *
 * @author Aurimas Blažulionis
 */
public class UIMaterial implements IMaterial, Serializable {
    public static class UIShaderSet extends ShaderSet {
        public UIShaderSet() {
            mVertexShader = ShaderBuf.getResource("ui", ShaderKind.VERTEX_SHADER);
            mFragmentShader = ShaderBuf.getResource("ui", ShaderKind.FRAGMENT_SHADER);

            mRenderOrder = ShaderSet.RenderOrder.UI.getValue();
            mDepthTest = false;
            mAlphaBlend = true;
            mPreSort = true;

            mVertexBindingDescription = BindingDescription.instancedWithMatrix(4 * 4);
            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            new AttributeDescription(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 0));
            mNumFragmentTextures = 1;
        }
    }

    private static UIShaderSet sShaderSet = new UIShaderSet();

    private SampledTexture[] mFragmentTextures = {
        new SampledTexture(
                Texture.getResource("white.bmp"),
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT))
    };

    /** Colour of the surface. It will multiply the texture's colour */
    public Vector4f colour = new Vector4f(1.f);

    public UIMaterial() {}

    public UIMaterial(Vector4fc colour, SampledTexture texture) {
        this.colour.set(colour);
        mFragmentTextures[0] = texture;
    }

    public UIMaterial(Vector4fc colour) {
        this.colour.set(colour);
    }

    public UIMaterial(SampledTexture texture) {
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
