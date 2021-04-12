/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderBuf.MacroDefinition;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Reference PBR material
 *
 * <p>This material provides physically based rendering of objects with textures.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class PBRMaterial implements IColouredMaterial, IRefCountedMaterial, Serializable {

    private static final Map<Integer, ShaderSet> sShaderSets = new TreeMap<Integer, ShaderSet>();

    public static class StandardShaderSet extends ShaderSet {

        public StandardShaderSet(PBRMaterial mat) {
            this(mat, "standard", new ArrayList<>(), new ArrayList<>(), 0);
        }

        public StandardShaderSet(
                PBRMaterial mat,
                String shaderName,
                List<MacroDefinition> fragMacroDefs,
                List<MacroDefinition> vertMacroDefs,
                int extraInstanceDataSize,
                AttributeDescription... extraDescriptions) {

            int textureCount = 0;
            if (mat.mAlbedoMap != null) {
                fragMacroDefs.add(
                        new MacroDefinition("ALBEDO_BINDING", Integer.toString(textureCount++)));
            }
            if (mat.mNormalMap != null) {
                fragMacroDefs.add(
                        new MacroDefinition("NORMAL_BINDING", Integer.toString(textureCount++)));
            }
            if (mat.mMetalnessRoughnessMap != null) {
                fragMacroDefs.add(
                        new MacroDefinition(
                                "METALNESS_ROUGHNESS_BINDING", Integer.toString(textureCount++)));
            }

            mNumFragmentTextures = textureCount;

            mLightCount = 2;

            MacroDefinition lights =
                    new MacroDefinition("NUM_LIGHTS", Integer.toString(mLightCount));
            fragMacroDefs.add(lights);
            vertMacroDefs.add(lights);

            if (mat.mAlphaBlend) {
                mRenderOrder = ShaderSet.RenderOrder.TRANSPARENT.getValue();
                mAlphaBlend = true;
                mPreSort = true;
                fragMacroDefs.add(new MacroDefinition("ALPHA_BLEND", "1"));
            }

            mVertexShader =
                    ShaderBuf.getResource(
                            shaderName,
                            ShaderKind.VERTEX_SHADER,
                            vertMacroDefs.stream().toArray(MacroDefinition[]::new));
            mFragmentShader =
                    ShaderBuf.getResource(
                            shaderName,
                            ShaderKind.FRAGMENT_SHADER,
                            fragMacroDefs.stream().toArray(MacroDefinition[]::new));

            mVertexBindingDescription =
                    BindingDescription.instancedWithMatrixAndLights(
                            NORMAL_OFFSET + 4 + extraInstanceDataSize, mLightCount);

            ArrayList<AttributeDescription> descriptions = new ArrayList<>();

            AttributeDescription[] regAttributes = {
                new AttributeDescription(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, COL_OFFSET),
                new AttributeDescription(1, 1, VK_FORMAT_R32G32B32_SFLOAT, CAM_OFFSET),
                new AttributeDescription(1, 2, VK_FORMAT_R32_SFLOAT, ALPHA_CUTOFF_OFFSET),
                new AttributeDescription(1, 3, VK_FORMAT_R32_SFLOAT, METALLIC_OFFSET),
                new AttributeDescription(1, 4, VK_FORMAT_R32_SFLOAT, ROUGHNESS_OFFSET),
                new AttributeDescription(1, 5, VK_FORMAT_R32_SFLOAT, NORMAL_OFFSET)
            };

            for (AttributeDescription desc : regAttributes) descriptions.add(desc);

            int binding = 5;

            for (AttributeDescription desc : extraDescriptions)
                descriptions.add(
                        new AttributeDescription(
                                1, ++binding, desc.format, NORMAL_OFFSET + 4 + desc.offset));

            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            AttributeDescription.withLights(
                                    mLightCount,
                                    descriptions.stream().toArray(AttributeDescription[]::new)));
        }
    }

    protected int hashShaderSet() {
        int ret = 0;
        ret |= mAlbedoMap != null ? 1 : 0;
        ret <<= 1;
        ret |= mNormalMap != null ? 1 : 0;
        ret <<= 1;
        ret |= mMetalnessRoughnessMap != null ? 1 : 0;
        ret <<= 1;
        ret |= mAlphaBlend ? 1 : 0;
        return ret;
    }

    private static final int COL_OFFSET = 0;
    private static final int CAM_OFFSET = COL_OFFSET + 4 * 4;
    private static final int ALPHA_CUTOFF_OFFSET = CAM_OFFSET + 3 * 4;
    private static final int METALLIC_OFFSET = ALPHA_CUTOFF_OFFSET + 4;
    private static final int ROUGHNESS_OFFSET = METALLIC_OFFSET + 4;
    private static final int NORMAL_OFFSET = ROUGHNESS_OFFSET + 4;

    @Getter protected SampledTexture mAlbedoMap;
    @Getter protected SampledTexture mNormalMap;
    @Getter protected SampledTexture mMetalnessRoughnessMap;

    protected SampledTexture[] mFragmentTextures;

    /** Base colour of the surface. It will multiply the texture's colour. */
    @Getter private final Vector4f mColour = new Vector4f(1.f);
    /** Controls which alpha values are cut off. */
    @Getter @Setter private float mAlphaCutoff = 0f;
    /** Metalicness multiplier. */
    @Getter @Setter private float mMetallic = 1f;
    /** Roughness multiplier. */
    @Getter @Setter private float mRoughness = 1f;
    /** Normal map multiplier. */
    @Getter @Setter private float mNormal = 1f;
    /** Have transparency. */
    @Getter @Setter private boolean mAlphaBlend = false;

    private int mRefCount = 0;

    /** Constructor for StandardMaterial. */
    public PBRMaterial() {}

    /**
     * Constructor for StandardMaterial.
     *
     * @param albedoMap initial albedo/diffuse texture of the object
     */
    public PBRMaterial(SampledTexture albedoMap) {
        mAlbedoMap = albedoMap;
    }

    /**
     * Constructor for StandardMaterial.
     *
     * @param albedoMap initial texture of the object
     * @param colour colour of the material
     */
    public PBRMaterial(SampledTexture albedoMap, Vector4f colour) {
        mAlbedoMap = albedoMap;
        mColour.set(colour);
    }

    /**
     * Constructor for StandardMaterial.
     *
     * @param colour colour of the material
     */
    public PBRMaterial(Vector4f colour) {
        mColour.set(colour);
    }

    public void setAlbedoMap(SampledTexture tex) {
        if (equalTexs(tex, mAlbedoMap)) {
            return;
        }

        if (mAlbedoMap != null) {
            mAlbedoMap.free();
        }
        mAlbedoMap = tex != null ? tex.clone() : null;
        mFragmentTextures = null;
    }

    public void setNormalMap(SampledTexture tex) {
        if (equalTexs(tex, mNormalMap)) {
            return;
        }

        if (mNormalMap != null) {
            mNormalMap.free();
        }

        if (tex != null) {
            mNormalMap = tex.clone();
            mNormalMap.setLinear(true);
            mFragmentTextures = null;
        } else if (mNormalMap != null) {
            mNormalMap = null;
            mFragmentTextures = null;
        }
    }

    public void setMetalnessRoughnessMap(SampledTexture tex) {
        if (equalTexs(tex, mMetalnessRoughnessMap)) {
            return;
        }

        if (mMetalnessRoughnessMap != null) {
            mMetalnessRoughnessMap.free();
        }

        if (tex != null) {
            mMetalnessRoughnessMap = tex.clone();
            mMetalnessRoughnessMap.setLinear(true);
            mFragmentTextures = null;
        } else if (mMetalnessRoughnessMap != null) {
            mMetalnessRoughnessMap = null;
            mFragmentTextures = null;
        }
    }

    public ShaderSet getShaderSet() {
        Integer hash = hashShaderSet();

        ShaderSet ret = sShaderSets.get(hash);

        if (ret == null) {
            ret = new StandardShaderSet(this);
            sShaderSets.put(hash, ret);
        }

        return ret;
    }

    public int writeVertexInstanceData(
            int offset, ByteBuffer buffer, Matrix4fc matrix, List<Light> lights) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        offset = Light.writeLights(offset, buffer, lights, getShaderSet().getLightCount());
        Scene.getActiveScene()
                .getSingleton(Camera.class)
                .getGameObject()
                .getTransform()
                .getPosition()
                .get(offset + CAM_OFFSET, buffer);
        mColour.get(offset + COL_OFFSET, buffer);
        ByteBuffer buf = (ByteBuffer) buffer.position(offset + ALPHA_CUTOFF_OFFSET);
        buf.putFloat(mAlphaCutoff);
        buf.putFloat(mMetallic);
        buf.putFloat(mRoughness);
        buf.putFloat(mNormal);
        return buf.position();
    }

    public SampledTexture[] getFragmentTextures() {
        if (mFragmentTextures == null) {
            mFragmentTextures =
                    Stream.of(mAlbedoMap, mNormalMap, mMetalnessRoughnessMap)
                            .filter(e -> e != null)
                            .toArray(SampledTexture[]::new);
        }

        return mFragmentTextures;
    }

    public IRefCountedMaterial incRefCount() {
        mRefCount++;
        return this;
    }

    public void free() {
        if (--mRefCount < 0) {
            if (mAlbedoMap != null) {
                mAlbedoMap.free();
            }
            if (mNormalMap != null) {
                mNormalMap.free();
            }
            if (mMetalnessRoughnessMap != null) {
                mMetalnessRoughnessMap.free();
            }
        }
    }

    private boolean equalTexs(SampledTexture a, SampledTexture b) {
        if ((a == null) != (b == null)) {
            return false;
        }
        if (a != null) {
            return a.equals(b);
        }
        return true;
    }
}
