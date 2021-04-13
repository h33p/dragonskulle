/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.materials;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
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

    private static final Map<Integer, ShaderSet> sShaderSets = new TreeMap<Integer, ShaderSet>();

    public static class HighlightShaderSet extends StandardShaderSet {
        public HighlightShaderSet(PBRHighlightMaterial mat) {
            super(
                    mat,
                    "highlight_pbr",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    OVERLAY_COL_OFFSET + 4 * 4,
                    new AttributeDescription(
                            1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, OVERLAY_COL_OFFSET));
        }
    }

    private static final int OVERLAY_COL_OFFSET = 0;

    @Getter private Vector4f mOverlayColour = new Vector4f(0f);

    @Override
    protected int hashShaderSet() {
        return super.hashShaderSet();
    }

    /** Constructor for StandardMaterial */
    public PBRHighlightMaterial() {
        super();
    }

    /**
     * Constructor for {@link PBRHighlightMaterial}
     *
     * <p>This constructor accepts a {@link PBRMaterial}, and clones its values
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
     * Constructor for StandardMaterial
     *
     * @param albedoMap initial albedo/diffuse texture of the object
     */
    public PBRHighlightMaterial(SampledTexture albedoMap) {
        super(albedoMap);
    }

    /**
     * Constructor for StandardMaterial
     *
     * @param albedoMap initial texture of the object
     * @param colour colour of the material
     */
    public PBRHighlightMaterial(SampledTexture albedoMap, Vector4f colour) {
        super(albedoMap, colour);
    }

    /**
     * Constructor for StandardMaterial
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
        return offset + 4 * 4;
    }

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
