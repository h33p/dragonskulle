/* (C) 2021 DragonSkulle */

package org.dragonskulle.ui;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.renderer.TextureMapping.TextureWrapping;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.materials.IMaterial;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
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
    public Vector4f mColour = new Vector4f(1.f);

    public UIMaterial() {}

    /**
     * Constructor for UIMaterial.
     *
     * @param colour initial colour value of the object
     * @param texture initial texture of the object
     */
    public UIMaterial(Vector4fc colour, SampledTexture texture) {
        this.mColour.set(colour);
        mFragmentTextures[0] = texture;
    }

    /**
     * Constructor for UIMaterial.
     *
     * @param colour initial colour value for the object, with full alpha
     * @param texture initial texture of the object
     */
    public UIMaterial(Vector3fc colour, SampledTexture texture) {
        this(new Vector4f(colour, 1.f), texture);
    }

    /**
     * Constructor for UIMaterial.
     *
     * @param colour initial colour value of the object
     */
    public UIMaterial(Vector4fc colour) {
        this.mColour.set(colour);
    }

    /**
     * Constructor for UIMaterial.
     *
     * @param colour initial colour value for the object, with full alpha
     */
    public UIMaterial(Vector3fc colour) {
        this(new Vector4f(colour, 1.f));
    }

    /**
     * Constructor for UIMaterial.
     *
     * @param texture initial texture of the object
     */
    public UIMaterial(SampledTexture texture) {
        mFragmentTextures[0] = texture;
    }

    public ShaderSet getShaderSet() {
        return sShaderSet;
    }

    public void writeVertexInstanceData(
            int offset, ByteBuffer buffer, Matrix4fc matrix, List<Light> lights) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        mColour.get(offset, buffer);
    }

    public SampledTexture[] getFragmentTextures() {
        return mFragmentTextures;
    }

    public void free() {
        for (SampledTexture tex : mFragmentTextures) {
            tex.free();
        }
    }
}
