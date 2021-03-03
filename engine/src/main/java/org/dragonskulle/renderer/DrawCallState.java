/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Renderable;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;

/**
 * Class abstracting a single draw call
 *
 * <p>This stores all data that is unique per draw.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class DrawCallState implements NativeResource {
    private VulkanShaderDescriptorPool mDescriptorPool;
    @Getter private ShaderSet mShaderSet;
    @Getter private VulkanPipeline mPipeline;
    private long mTextureSetLayout;
    private TextureSetFactory mTextureSetFactory;
    private VulkanSampledTextureFactory mTextureFactory;
    private Mesh mMesh;
    @Getter private VulkanMeshBuffer.MeshDescriptor mMeshDescriptor;
    @Getter private Map<TextureHashKey, DrawData> mDrawData = new HashMap<>();

    private int mDescriptorSetCount = 0;

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
            mShaderSet = renderable.material.getShaderSet();
            mMesh = renderable.mesh;
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
                object.material.writeVertexInstanceData(cur_off, buffer, object.matrix);
                cur_off += shaderSet.getVertexBindingDescription().size;
            }
        }

        public int setInstanceBufferOffset(ShaderSet shaderSet, int offset) {
            mInstanceBufferOffset = offset;
            return offset + shaderSet.getVertexBindingDescription().size * mObjects.size();
        }

        public void endDrawData(int imageIndex, Long descriptorSet) {
            int descriptorSetIndex = 0;

            if (descriptorSet != null) mDescriptorSets[descriptorSetIndex++] = descriptorSet;
            if (mTextureSet != null)
                mDescriptorSets[descriptorSetIndex++] = mTextureSet.getDescriptorSet(imageIndex);
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

    public DrawCallState(
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
        // mDrawData = new DrawData();
        mDescriptorPool =
                VulkanShaderDescriptorPool.createPool(
                        device, physicalDevice, shaderSet, imageCount);
        mShaderSet = shaderSet;
        mMesh = mesh;
        mTextureSetFactory = textureSetFactory;
        mTextureFactory = textureFactory;

        if (shaderSet.getNumFragmentTextures() > 0) {
            mTextureSetLayout = layoutFactory.getLayout(shaderSet.getNumFragmentTextures());
        }

        mDescriptorSetCount = 0;
        if (mDescriptorPool != null) mDescriptorSetCount++;
        if (mTextureSetLayout != 0) mDescriptorSetCount++;

        mPipeline =
                new VulkanPipeline(
                        shaderSet,
                        mDescriptorPool == null ? null : mDescriptorPool.getSetLayout(),
                        mTextureSetLayout == 0 ? null : mTextureSetLayout,
                        device,
                        extent,
                        renderPass);
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
        mTmpTextureHashKey.setMaterial(object.material);
        DrawData drawData = mDrawData.get(mTmpTextureHashKey);
        // If we never had this texture set, create a pool
        if (drawData == null) {
            drawData = new DrawData();
            if (mDescriptorSetCount > 0) drawData.mDescriptorSets = new long[mDescriptorSetCount];
            if (mShaderSet.mNumFragmentTextures > 0) {
                SampledTexture[] matTextures = object.material.getFragmentTextures();
                SampledTexture[] textures = new SampledTexture[mShaderSet.getNumFragmentTextures()];
                int matTexturesLength = matTextures.length;

                for (int i = 0; i < textures.length; i++) {
                    if (i < matTexturesLength) textures[i] = matTextures[i];
                    if (textures[i] == null) textures[i] = ERROR_TEXTURE;
                }

                drawData.mTextureSet = mTextureSetFactory.getSet(textures, mTextureFactory);
            }
            mDrawData.put(new TextureHashKey(object.material), drawData);
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
}
