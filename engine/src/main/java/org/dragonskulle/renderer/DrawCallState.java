/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Renderable;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;

/**
 * Class abstracting a single graphics pipeline
 *
 * <p>This stores all data that is unique per material/pipeline, along with lists of objects that
 * are drawn using instanced rendering.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class DrawCallState implements NativeResource {
    private VulkanShaderDescriptorPool mDescriptorPool;
    @Getter private ShaderSet mShaderSet;
    @Getter private VulkanPipeline mPipeline;
    private TextureSetFactory mTextureSetFactory;
    private VulkanSampledTextureFactory mTextureFactory;
    private Mesh mMesh;
    @Getter private VulkanMeshBuffer.MeshDescriptor mMeshDescriptor;
    private Map<TextureHashKey, DrawData> mDrawData = new HashMap<>();

    private TextureHashKey mTmpTextureHashKey = new TextureHashKey();

    private static class TextureHashKey {
        SampledTexture[] mMatTextures;

        private TextureHashKey() {}

        public TextureHashKey(IMaterial material) {
            setMaterial(material);
        }

        public void setMaterial(IMaterial material) {
            mMatTextures = material.getFragmentTextures();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(mMatTextures);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (!(other instanceof TextureHashKey)) return false;
            return Arrays.equals(mMatTextures, ((TextureHashKey) other).mMatTextures);
        }
    }

    public static class HashKey {
        ShaderSet mShaderSet;
        Mesh mMesh;

        public HashKey() {}

        public HashKey(Renderable renderable) {
            setRenderable(renderable);
        }

        public void setRenderable(Renderable renderable) {
            mShaderSet = renderable.getMaterial().getShaderSet();
            mMesh = renderable.getMesh();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mShaderSet, mMesh);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (!(other instanceof HashKey)) return false;
            HashKey otherKey = (HashKey) other;
            return mShaderSet.equals(otherKey.mShaderSet) && mMesh.equals(otherKey.mMesh);
        }
    }

    /** Describes addition data needed for a single instanced draw call */
    @Accessors(prefix = "m")
    @Getter
    public static class DrawData {
        private TextureSet mTextureSet;
        long[] mDescriptorSets;
        int mInstanceBufferOffset;

        List<Renderable> mObjects = new ArrayList<>();

        public void updateInstanceBuffer(ShaderSet shaderSet, ByteBuffer buffer) {
            int cur_off = mInstanceBufferOffset;
            for (Renderable object : mObjects) {
                object.writeVertexInstanceData(cur_off, buffer);
                cur_off += shaderSet.getVertexBindingDescription().size;
            }
        }

        public int setInstanceBufferOffset(ShaderSet shaderSet, int offset) {
            mInstanceBufferOffset = offset;
            return offset + shaderSet.getVertexBindingDescription().size * mObjects.size();
        }

        public void endDrawData(int imageIndex, Long descriptorSet) {
            mDescriptorSets =
                    combineDescriptorSets(
                            mDescriptorSets,
                            descriptorSet,
                            mTextureSet == null ? null : mTextureSet.getDescriptorSet(imageIndex));
        }
    }

    /** Specifies a non-instanced draw object. Mainly used in object pre-sorting */
    @Builder
    @Accessors(prefix = "m")
    @Getter
    public static class NonInstancedDraw {
        private DrawCallState mState;
        private DrawData mData;
        private int mObjectID;

        public int getInstanceBufferOffset() {
            ShaderSet shaderSet = mState.getShaderSet();
            return mData.getInstanceBufferOffset()
                    + shaderSet.getVertexBindingDescription().size * mObjectID;
        }
    }

    private static final SampledTexture ERROR_TEXTURE =
            new SampledTexture(
                    Texture.getResource("error.png"),
                    new TextureMapping(
                            TextureMapping.TextureFiltering.LINEAR,
                            TextureMapping.TextureWrapping.REPEAT));

    public DrawCallState(Renderer renderer, int imageCount, HashKey key) {
        this(
                renderer.getDevice(),
                renderer.getPhysicalDevice(),
                renderer.getExtent(),
                renderer.getRenderPass(),
                renderer.getTextureSetLayoutFactory(),
                renderer.getTextureSetFactory(),
                renderer.getTextureFactory(),
                imageCount,
                key.mShaderSet,
                key.mMesh);
    }

    private DrawCallState(
            VkDevice device,
            PhysicalDevice physicalDevice,
            VkExtent2D extent,
            long renderPass,
            TextureSetLayoutFactory layoutFactory,
            TextureSetFactory textureSetFactory,
            VulkanSampledTextureFactory textureFactory,
            int imageCount,
            ShaderSet shaderSet,
            Mesh mesh) {
        mDescriptorPool =
                VulkanShaderDescriptorPool.createPool(
                        device, physicalDevice, shaderSet, imageCount);
        mShaderSet = shaderSet;
        mMesh = mesh;
        mTextureSetFactory = textureSetFactory;
        mTextureFactory = textureFactory;

        long[] descriptorSetLayouts =
                combineDescriptorSetLayouts(
                        mDescriptorPool == null ? null : mDescriptorPool.getSetLayout(),
                        shaderSet.getNumFragmentTextures() < 0
                                ? null
                                : layoutFactory.getLayout(shaderSet.getNumFragmentTextures()));

        mPipeline = new VulkanPipeline(shaderSet, descriptorSetLayouts, device, extent, renderPass);
    }

    public Collection<DrawData> getDrawData() {
        return mDrawData.values();
    }

    public void startDrawData(VulkanMeshBuffer meshBuffer) {
        mMeshDescriptor = meshBuffer.addMesh(mMesh);

        for (DrawData d : mDrawData.values()) d.mObjects.clear();
    }

    public void endDrawData(int imageIndex) {
        Long descriptorSet =
                mDescriptorPool == null ? null : mDescriptorPool.getDescriptorSet(imageIndex);
        for (DrawData d : mDrawData.values()) d.endDrawData(imageIndex, descriptorSet);
    }

    public void addObject(Renderable object) {
        IMaterial material = object.getMaterial();
        mTmpTextureHashKey.setMaterial(material);
        DrawData drawData = mDrawData.get(mTmpTextureHashKey);
        // If we never had this texture set, create a pool
        if (drawData == null) {
            drawData = new DrawData();
            if (mShaderSet.mNumFragmentTextures > 0) {
                SampledTexture[] matTextures = material.getFragmentTextures();
                SampledTexture[] textures = new SampledTexture[mShaderSet.getNumFragmentTextures()];
                int matTexturesLength = matTextures.length;

                for (int i = 0; i < textures.length; i++) {
                    if (i < matTexturesLength) textures[i] = matTextures[i];
                    if (textures[i] == null) textures[i] = ERROR_TEXTURE;
                }

                drawData.mTextureSet = mTextureSetFactory.getSet(textures, mTextureFactory);
            }
            mDrawData.put(new TextureHashKey(material), drawData);
        }
        drawData.mObjects.add(object);
    }

    public int setInstanceBufferOffset(int offset) {
        for (DrawData d : mDrawData.values())
            offset = d.setInstanceBufferOffset(mShaderSet, offset);
        return offset;
    }

    public void updateInstanceBuffer(ByteBuffer buffer) {
        for (DrawData d : mDrawData.values()) d.updateInstanceBuffer(mShaderSet, buffer);
    }

    @Override
    public void free() {
        if (mPipeline != null) mPipeline.free();
        mPipeline = null;
    }

    private static long[] combineDescriptorSets(long[] arr, Long... entries) {
        int elemCount = 0;
        for (Long l : entries) if (l != null) elemCount++;
        long[] ret = (arr != null && elemCount == arr.length) ? arr : new long[elemCount];
        elemCount = 0;
        for (Long l : entries) if (l != null) ret[elemCount++] = l;
        return ret;
    }

    private static long[] combineDescriptorSetLayouts(Long... layouts) {
        return combineDescriptorSets(null, layouts);
    }
}
