/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.materials;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
import org.dragonskulle.renderer.materials.IColouredMaterial;
import org.dragonskulle.renderer.materials.IMaterial;
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
public class VertexHighlightMaterial implements IMaterial, IColouredMaterial, Serializable {
    public static class MatShaderSet extends ShaderSet {
        public MatShaderSet() {
            mVertexShader = ShaderBuf.getResource("vertex_highlight", ShaderKind.VERTEX_SHADER);
            mFragmentShader = ShaderBuf.getResource("vertex_highlight", ShaderKind.FRAGMENT_SHADER);

            final int COLOUR_SIZE = 4 * 4;
            final int FLOAT_SIZE = 4;

            mVertexBindingDescription =
                    BindingDescription.instancedWithMatrix(COLOUR_SIZE * 2 + 2 * FLOAT_SIZE);
            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            new AttributeDescription(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 0),
                            new AttributeDescription(
                                    1, 1, VK_FORMAT_R32G32B32A32_SFLOAT, COLOUR_SIZE),
                            new AttributeDescription(1, 2, VK_FORMAT_R32_SFLOAT, COLOUR_SIZE * 2),
                            new AttributeDescription(
                                    1, 3, VK_FORMAT_R32_SFLOAT, COLOUR_SIZE * 2 + FLOAT_SIZE));
            mNumFragmentTextures = 1;
        }

        public MatShaderSet enableAlpha() {
            // TODO: add order independent transparency
            mRenderOrder = ShaderSet.RenderOrder.TRANSPARENT.getValue();
            mAlphaBlend = true;
            mPreSort = true;
            return this;
        }
    }

    private static final MatShaderSet OPAQUE_SET = new MatShaderSet();
    private static final MatShaderSet TRANSPARENT_SET = new MatShaderSet().enableAlpha();

    protected SampledTexture[] mFragmentTextures = {
        new SampledTexture(
                Texture.getResource("test_cc0_texture.jpg"),
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT))
    };

    /** Colour of the vertex highlight. It will multiply the texture's colour */
    @Getter private final Vector4f mColour = new Vector4f(0.5f);
    /** Colour of the vertex highlight. It will multiply the texture's colour */
    @Getter private final Vector4f mTexColour = new Vector4f(1f);

    @Getter @Setter private float mDistancePow = 80.f;
    @Getter @Setter private float mVertexDistance = 1f;

    private int mRefCount = 0;

    /** Constructor for VertexHighlightMaterial. */
    public VertexHighlightMaterial() {}

    /**
     * Constructor for VertexHighlightMaterial.
     *
     * @param texture initial texture of the object
     */
    public VertexHighlightMaterial(SampledTexture texture) {
        mFragmentTextures[0] = texture;
    }

    /**
     * Constructor for VertexHighlightMaterial.
     *
     * @param texture initial texture of the object
     * @param colour colour of the material
     */
    public VertexHighlightMaterial(SampledTexture texture, Vector4f colour) {
        mFragmentTextures[0] = texture;
        mColour.set(colour);
    }

    /**
     * Constructor for VertexHighlightMaterial.
     *
     * @param colour colour of the material
     */
    public VertexHighlightMaterial(Vector4f colour) {
        mColour.set(colour);
    }

    public ShaderSet getShaderSet() {
        if (mTexColour.w < 1f) {
            return TRANSPARENT_SET;
        }
        return OPAQUE_SET;
    }

    public void writeVertexInstanceData(
            int offset, ByteBuffer buffer, Matrix4fc matrix, List<Light> lights) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        mColour.get(offset, buffer);
        mTexColour.get(offset + 4 * 4, buffer);
        buffer.putFloat(offset + 4 * 8, mDistancePow);
        buffer.putFloat(offset + 4 * 9, mVertexDistance);
    }

    public SampledTexture[] getFragmentTextures() {
        return mFragmentTextures;
    }

    public VertexHighlightMaterial incRefCount() {
        mRefCount++;
        return this;
    }

    public void free() {
        if (--mRefCount < 0) {
            for (SampledTexture tex : mFragmentTextures) {
                tex.free();
            }
        }
    }
}
