/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.*;
import org.dragonskulle.renderer.components.Light;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Reference unlit material
 *
 * <p>This material provides basic unlit rendering of objects with textures. One texture is
 * settable, alongside per-object colour value.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UnlitMaterial
        implements IMaterial, IColouredMaterial, IRefCountedMaterial, Serializable {
    public static class UnlitShaderSet extends ShaderSet {
        /** Creates a unlit shader set */
        public UnlitShaderSet() {
            mVertexShader = ShaderBuf.getResource("unlit", ShaderKind.VERTEX_SHADER);
            mFragmentShader = ShaderBuf.getResource("unlit", ShaderKind.FRAGMENT_SHADER);

            mVertexBindingDescription = BindingDescription.instancedWithMatrix(4 * 4);
            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            new AttributeDescription(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 0));
            mNumFragmentTextures = 1;
        }

        /**
         * Enables ordered alpha blending for the shader set
         *
         * @return this
         */
        public UnlitShaderSet enableAlpha() {
            // TODO: add order independent transparency
            mRenderOrder = ShaderSet.RenderOrder.TRANSPARENT.getValue();
            mAlphaBlend = true;
            mPreSort = true;
            return this;
        }
    }

    /** Shader set used for opaque unlit objects */
    private static final UnlitShaderSet OPAQUE_SET = new UnlitShaderSet();
    /** Shader set used for transparent unlit objects */
    private static final UnlitShaderSet TRANSPARENT_SET = new UnlitShaderSet().enableAlpha();

    /** Fragment texture used for drawing */
    protected SampledTexture[] mFragmentTextures = {
        new SampledTexture(
                Texture.getResource("test_cc0_texture.jpg"),
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT))
    };

    /** Colour of the surface. It will multiply the texture's colour */
    @Getter private final Vector4f mColour = new Vector4f(1.f);

    /** The internal reference count */
    private int mRefCount = 0;

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

    /**
     * Constructor for UnlitMaterial
     *
     * @param texture initial texture of the object
     * @param colour colour of the material
     */
    public UnlitMaterial(SampledTexture texture, Vector4f colour) {
        mFragmentTextures[0] = texture;
        mColour.set(colour);
    }

    /**
     * Constructor for UnlitMaterial
     *
     * @param colour colour of the material
     */
    public UnlitMaterial(Vector4f colour) {
        mColour.set(colour);
    }

    /**
     * Gets the shader set of this material
     *
     * <p>TODO: probably enable alpha blending the same way it's handled in PBRMaterial
     *
     * @return {@code OPAQUE_SET}, if the colour alpha is 1, {@code TRANSPARENT_SET} otherwise
     */
    public ShaderSet getShaderSet() {
        if (mColour.w < 1f) return TRANSPARENT_SET;
        return OPAQUE_SET;
    }

    /**
     * Writes the vertex shader instance data
     *
     * @param offset where to write the data to inside the buffer
     * @param buffer where to write the data to
     * @param matrix the world space matrix of the object
     * @param lights the world lights (unused)
     */
    public void writeVertexInstanceData(
            int offset, ByteBuffer buffer, Matrix4fc matrix, List<Light> lights) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        mColour.get(offset, buffer);
    }

    /**
     * Gets the list of fragment shader textures used. It should be the same size as {@link
     * ShaderSet#mNumFragmentTextures}
     *
     * @return the array of SampledTexture
     */
    public SampledTexture[] getFragmentTextures() {
        return mFragmentTextures;
    }

    /**
     * Increase the reference count and return the material
     *
     * @return this
     */
    public IRefCountedMaterial incRefCount() {
        mRefCount++;
        return this;
    }

    /**
     * Free the material. It will release fragment textures if the reference count drops below zero
     */
    public void free() {
        if (--mRefCount < 0) for (SampledTexture tex : mFragmentTextures) tex.free();
    }
}
