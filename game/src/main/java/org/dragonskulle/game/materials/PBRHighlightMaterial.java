/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.materials;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * PBR material with highlights
 *
 * <p>This material provides basic unlit rendering of objects with textures. One texture is
 * settable, alongside per-object colour value.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class PBRHighlightMaterial extends PBRMaterial {

    /** Map from various material properties and cached shadersets. */
    private static final Map<Integer, ShaderSet> sShaderSets = new TreeMap<Integer, ShaderSet>();

    /**
     * This is a modified shader set that appends extra options on top of the {@link
     * StandardShaderSet}.
     */
    public static class HighlightShaderSet extends StandardShaderSet {
        /**
         * Constructor for {@link HighlightShaderSet}.
         *
         * @param mat material to create the shader set for.
         */
        public HighlightShaderSet(PBRHighlightMaterial mat) {
            super(
                    mat,
                    "highlight_pbr",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    OVERLAY_ALPHAMUL_OFFSET + 4 * 4,
                    new AttributeDescription(
                            1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, OVERLAY_COL_OFFSET),
                    new AttributeDescription(1, 1, VK_FORMAT_R32_SFLOAT, OVERLAY_MINDIST_OFFSET),
                    new AttributeDescription(1, 2, VK_FORMAT_R32_SFLOAT, OVERLAY_MAXDIST_OFFSET),
                    new AttributeDescription(1, 3, VK_FORMAT_R32_SFLOAT, OVERLAY_MINLERP_OFFSET),
                    new AttributeDescription(1, 4, VK_FORMAT_R32_SFLOAT, OVERLAY_DISTPOW_OFFSET),
                    new AttributeDescription(1, 5, VK_FORMAT_R32_SFLOAT, OVERLAY_ALPHAMUL_OFFSET));
        }
    }

    /** Offset from the end of previous buffer to colour component. */
    private static final int OVERLAY_COL_OFFSET = 0;
    /** Offset from the end of previous buffer to minimum distance component. */
    private static final int OVERLAY_MINDIST_OFFSET = OVERLAY_COL_OFFSET + 4 * 4;
    /** Offset from the end of previous buffer to maximum distance component. */
    private static final int OVERLAY_MAXDIST_OFFSET = OVERLAY_MINDIST_OFFSET + 4;
    /** Offset from the end of previous buffer to minimum lerp component. */
    private static final int OVERLAY_MINLERP_OFFSET = OVERLAY_MAXDIST_OFFSET + 4;
    /** Offset from the end of previous buffer to distance power component. */
    private static final int OVERLAY_DISTPOW_OFFSET = OVERLAY_MINLERP_OFFSET + 4;
    /** Offset from the end of previous buffer to alpha multiply component. */
    private static final int OVERLAY_ALPHAMUL_OFFSET = OVERLAY_DISTPOW_OFFSET + 4;

    @Getter private Vector4f mOverlayColour = new Vector4f(0f);

    /** Distance at which mMinLerp highlight will be used. */
    @Getter @Setter private float mMinDist = 0.1f;
    /** Distance at which highlight of lerp value 1 will be used. */
    @Getter @Setter private float mMaxDist = 0.32f;
    /**
     * Interpolation value at mMinDist. Higher values will yield more overlay on all parts of the
     * object.
     */
    @Getter @Setter private float mMinLerp = 1f;
    /** How much increasing distance increases highlighting. */
    @Getter @Setter private float mDistPow = 5f;
    /** Alpha multiplier in the shader. */
    @Getter @Setter private float mAlphaMul = 4f;

    @Override
    protected int hashShaderSet() {
        return super.hashShaderSet();
    }

    /** Constructor for {@link PBRHighlightMaterial}. */
    public PBRHighlightMaterial() {
        super();
    }

    /**
     * Constructor for {@link PBRHighlightMaterial}.
     *
     * <p>This constructor accepts a {@link PBRMaterial}, and clones its values
     *
     * @param pbrMat material to clone.
     */
    public PBRHighlightMaterial(PBRMaterial pbrMat) {
        setAlbedoMap(pbrMat.getAlbedoMap());
        setNormalMap(pbrMat.getNormalMap());
        setMetalnessRoughnessMap(pbrMat.getMetalnessRoughnessMap());

        getColour().set(pbrMat.getColour());
        setAlphaCutoff(pbrMat.getAlphaCutoff());
        setMetallic(pbrMat.getMetallic());
        setRoughness(pbrMat.getRoughness());
        setNormal(pbrMat.getNormal());
        setAlphaBlend(pbrMat.isAlphaBlend());
    }

    /**
     * Constructor for {@link PBRHighlightMaterial}.
     *
     * @param albedoMap initial albedo/diffuse texture of the object
     */
    public PBRHighlightMaterial(SampledTexture albedoMap) {
        super(albedoMap);
    }

    /**
     * Constructor for {@link PBRHighlightMaterial}.
     *
     * @param albedoMap initial texture of the object
     * @param colour colour of the material
     */
    public PBRHighlightMaterial(SampledTexture albedoMap, Vector4f colour) {
        super(albedoMap, colour);
    }

    /**
     * Constructor for {@link PBRHighlightMaterial}.
     *
     * @param colour colour of the material
     */
    public PBRHighlightMaterial(Vector4f colour) {
        super(colour);
    }

    @Override
    public int writeVertexInstanceData(
            int offset, ByteBuffer buffer, Matrix4fc matrix, List<Light> lights) {
        offset = super.writeVertexInstanceData(offset, buffer, matrix, lights);
        mOverlayColour.get(offset + OVERLAY_COL_OFFSET, buffer);
        buffer.putFloat(offset + OVERLAY_MINDIST_OFFSET, mMinDist);
        buffer.putFloat(offset + OVERLAY_MAXDIST_OFFSET, mMaxDist);
        buffer.putFloat(offset + OVERLAY_MINLERP_OFFSET, mMinLerp);
        buffer.putFloat(offset + OVERLAY_DISTPOW_OFFSET, mDistPow);
        buffer.putFloat(offset + OVERLAY_ALPHAMUL_OFFSET, mAlphaMul);
        return offset + OVERLAY_ALPHAMUL_OFFSET + 4;
    }

    @Override
    public ShaderSet getShaderSet() {
        Integer hash = hashShaderSet();

        ShaderSet ret = sShaderSets.get(hash);

        if (ret == null) {
            ret = new HighlightShaderSet(this);
            sShaderSets.put(hash, ret);
        }

        return ret;
    }
}
