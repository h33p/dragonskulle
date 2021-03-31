/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

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
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.IMaterial;
import org.lwjgl.PointerBuffer;
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
    private VkDevice mDevice;
    private VulkanShaderDescriptorPool mDescriptorPool;
    @Getter private ShaderSet mShaderSet;
    @Getter private VulkanPipeline mPipeline;
    private TextureSetFactory mTextureSetFactory;
    private VulkanSampledTextureFactory mTextureFactory;
    private Map<DrawDataHashKey, DrawData> mDrawData = new HashMap<>();

    private DrawDataHashKey mTmpDrawDataHashKey = new DrawDataHashKey();

    private static class DrawDataHashKey {
        SampledTexture[] mMatTextures;
        Mesh mMesh;

        private DrawDataHashKey() {}

        public DrawDataHashKey(IMaterial material, Renderable renderable) {
            setData(material, renderable);
        }

        public void setData(IMaterial material, Renderable renderable) {
            mMatTextures = material.getFragmentTextures();
            mMesh = renderable.getMesh();
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(mMatTextures), mMesh);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (!(other instanceof DrawDataHashKey)) return false;
            DrawDataHashKey otherKey = (DrawDataHashKey) other;
            return Arrays.equals(mMatTextures, otherKey.mMatTextures)
                    && mMesh.equals(otherKey.mMesh);
        }
    }

    public static class HashKey {
        ShaderSet mShaderSet;

        public HashKey() {}

        public HashKey(Renderable renderable) {
            setRenderable(renderable);
        }

        public void setRenderable(Renderable renderable) {
            mShaderSet = renderable.getMaterial().getShaderSet();
        }

        @Override
        public int hashCode() {
            return mShaderSet.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (!(other instanceof HashKey)) return false;
            HashKey otherKey = (HashKey) other;
            return mShaderSet.equals(otherKey.mShaderSet);
        }
    }

    /** Describes addition data needed for a single instanced draw call */
    @Accessors(prefix = "m")
    @Getter
    public static class DrawData {
        private TextureSet mTextureSet;
        private Mesh mMesh;
        private VulkanMeshBuffer.MeshDescriptor mMeshDescriptor;
        private VkDevice mDevice;
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

        public void slowUpdateInstanceBuffer(
                ShaderSet shaderSet, PointerBuffer pData, long memory) {
            int shaderSetSize = shaderSet.getVertexBindingDescription().size;
            int cur_off = mInstanceBufferOffset;
            for (Renderable object : mObjects) {
                pData.rewind();
                int res = vkMapMemory(mDevice, memory, cur_off, shaderSetSize, 0, pData);

                if (res != VK_SUCCESS)
                    throw new RuntimeException(
                            String.format(
                                    "Failed to map memory! Out of resources! off: %x sz: %x",
                                    cur_off, shaderSetSize));

                ByteBuffer byteBuffer = pData.getByteBuffer(shaderSetSize);

                object.writeVertexInstanceData(0, byteBuffer);

                vkUnmapMemory(mDevice, memory);

                cur_off += shaderSetSize;
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
                renderer.getMSAASamples(),
                key.mShaderSet);
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
            int msaaCount,
            ShaderSet shaderSet) {
        mDevice = device;
        mDescriptorPool =
                VulkanShaderDescriptorPool.createPool(
                        device, physicalDevice, shaderSet, imageCount);
        mShaderSet = shaderSet;
        mTextureSetFactory = textureSetFactory;
        mTextureFactory = textureFactory;

        long[] descriptorSetLayouts =
                combineDescriptorSetLayouts(
                        mDescriptorPool == null ? null : mDescriptorPool.getSetLayout(),
                        shaderSet.getNumFragmentTextures() < 0
                                ? null
                                : layoutFactory.getLayout(shaderSet.getNumFragmentTextures()));

        mPipeline =
                new VulkanPipeline(
                        shaderSet, descriptorSetLayouts, device, extent, renderPass, msaaCount);
    }

    public Collection<DrawData> getDrawData() {
        return mDrawData.values();
    }

    public void startDrawData() {
        for (DrawData d : mDrawData.values()) d.mObjects.clear();

        mDrawData.entrySet().removeIf(e -> e.getValue().getMesh().getRefCount() <= 0);
    }

    public void endDrawData(int imageIndex) {
        Long descriptorSet =
                mDescriptorPool == null ? null : mDescriptorPool.getDescriptorSet(imageIndex);
        for (DrawData d : mDrawData.values()) d.endDrawData(imageIndex, descriptorSet);
    }

    public void addObject(Renderable object) {
        IMaterial material = object.getMaterial();
        mTmpDrawDataHashKey.setData(material, object);
        DrawData drawData = mDrawData.get(mTmpDrawDataHashKey);
        // If we never had this texture set and mesh combo, create a pool
        if (drawData == null) {
            drawData = new DrawData();
            drawData.mDevice = mDevice;
            if (mShaderSet.mNumFragmentTextures > 0) {
                SampledTexture[] matTextures = material.getFragmentTextures();
                SampledTexture[] textures = new SampledTexture[mShaderSet.getNumFragmentTextures()];
                int matTexturesLength = matTextures.length;

                for (int i = 0; i < textures.length; i++) {
                    if (i < matTexturesLength) textures[i] = matTextures[i];
                    if (textures[i] == null) textures[i] = ERROR_TEXTURE;
                }

                drawData.mTextureSet = mTextureSetFactory.getSet(textures, mTextureFactory);
                drawData.mMesh = object.getMesh();
            }
            mDrawData.put(new DrawDataHashKey(material, object), drawData);
        }
        drawData.mObjects.add(object);
    }

    public void updateMeshBuffer(VulkanMeshBuffer meshBuffer) {
        for (DrawData data : mDrawData.values())
            data.mMeshDescriptor = meshBuffer.addMesh(data.mMesh);
    }

    public int setInstanceBufferOffset(int offset) {
        for (DrawData d : mDrawData.values())
            offset = d.setInstanceBufferOffset(mShaderSet, offset);
        return offset;
    }

    public void updateInstanceBuffer(ByteBuffer buffer) {
        for (DrawData d : mDrawData.values()) d.updateInstanceBuffer(mShaderSet, buffer);
    }

    public void slowUpdateInstanceBuffer(PointerBuffer pData, long memory) {
        for (DrawData d : mDrawData.values()) d.slowUpdateInstanceBuffer(mShaderSet, pData, memory);
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
