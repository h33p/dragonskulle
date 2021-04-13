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
import org.dragonskulle.renderer.components.Light;
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
    /** Reference to the device this state is on */
    private VkDevice mDevice;
    /** Descriptor pool for this drawcall state */
    private VulkanShaderDescriptorPool mDescriptorPool;
    /** Reference to the shader set of the state */
    @Getter private ShaderSet mShaderSet;
    /** Vulkan pipeline used to render the objects with */
    @Getter private VulkanPipeline mPipeline;
    /** Reference to the texture set factory */
    private TextureSetFactory mTextureSetFactory;
    /** Reference to the texture factory */
    private VulkanSampledTextureFactory mTextureFactory;
    /** Maps object mesh and textures to instanced draw data */
    private Map<DrawDataHashKey, DrawData> mDrawData = new HashMap<>();

    /** Temporary hash key to avoid GC */
    private DrawDataHashKey mTmpDrawDataHashKey = new DrawDataHashKey();

    /** The hash key used for splitting objects up into instanced draws */
    private static class DrawDataHashKey {
        /** Textures used for the objects */
        SampledTexture[] mMatTextures;
        /** Mesh used for the objects */
        Mesh mMesh;

        private DrawDataHashKey() {}

        /**
         * Create the map key
         *
         * @param material the material that's used on the object
         * @param renderable the renderable of the object
         */
        public DrawDataHashKey(IMaterial material, Renderable renderable) {
            setData(material, renderable);
        }

        /**
         * Sets the map key data
         *
         * @param material the material that's used on the object
         * @param renderable the renderable of the object
         */
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

    /** Describes addition data needed for a single instanced draw call */
    @Accessors(prefix = "m")
    @Getter
    public static class DrawData {
        /** The textures that the fragment shader needs */
        private TextureSet mTextureSet;
        /** Mesh to draw */
        private Mesh mMesh;
        /** Descriptor of the mesh within the mesh buffer */
        private VulkanMeshBuffer.MeshDescriptor mMeshDescriptor;
        /** The device to draw on */
        private VkDevice mDevice;
        /** Descriptor sets that will be bound to the shader */
        long[] mDescriptorSets;
        /** Offset within the instance buffer where the per-instance data of this group begins */
        int mInstanceBufferOffset;

        /** Objects in this group */
        List<Renderable> mObjects = new ArrayList<>();

        /**
         * Update the instance buffer
         *
         * @param shaderSet the shader set of the parent supergroup
         * @param buffer the instance buffer
         * @param lights world lights
         */
        public void updateInstanceBuffer(
                ShaderSet shaderSet, ByteBuffer buffer, List<Light> lights) {
            int cur_off = mInstanceBufferOffset;
            for (Renderable object : mObjects) {
                object.writeVertexInstanceData(cur_off, buffer, lights);
                cur_off += shaderSet.getVertexBindingDescription().size;
            }
        }

        /**
         * Update the instance buffer
         *
         * <p>This is a fallback method that is used whenever mapping all instance buffer memory
         * fails
         *
         * @param shaderSet the shader set of the parent supergroup
         * @param pData temporary pointer
         * @param memory the memory address of the instance buffer
         * @param lights world lights
         */
        public void slowUpdateInstanceBuffer(
                ShaderSet shaderSet, PointerBuffer pData, long memory, List<Light> lights) {
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

                object.writeVertexInstanceData(0, byteBuffer, lights);

                vkUnmapMemory(mDevice, memory);

                cur_off += shaderSetSize;
            }
        }

        /**
         * Set the instance buffer offset and reserve space for itself
         *
         * @param shaderSet the shader set of the parent subgroup
         * @param offset the current offset witin the instance buffer
         * @return offset + any additional data that's needed for this group
         */
        public int setInstanceBufferOffset(ShaderSet shaderSet, int offset) {
            mInstanceBufferOffset = offset;
            return offset + shaderSet.getVertexBindingDescription().size * mObjects.size();
        }

        /**
         * Ends the draw data, sets the right descriptor sets
         *
         * @param imageIndex the image context index
         * @param descriptorSet optional descriptor set for uniform data
         */
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
        /** The super draw call state */
        private DrawCallState mState;
        /** The draw data for this object */
        private DrawData mData;
        /** The ID of this object */
        private int mObjectID;

        /** Retrieves the offset within the instance buffer for this object */
        public int getInstanceBufferOffset() {
            ShaderSet shaderSet = mState.getShaderSet();
            return mData.getInstanceBufferOffset()
                    + shaderSet.getVertexBindingDescription().size * mObjectID;
        }
    }

    /** The error texture that every failed to load texture is subsituded with */
    private static final SampledTexture ERROR_TEXTURE =
            new SampledTexture(
                    Texture.getResource("error.png"),
                    new TextureMapping(
                            TextureMapping.TextureFiltering.LINEAR,
                            TextureMapping.TextureWrapping.REPEAT));

    /**
     * Creates a draw call state
     *
     * @param renderer reference to the renderer that is performing the draws
     * @param imageCount the number of swapchain images used
     * @param shaderSet the shader set that will be used to draw the objects with
     */
    public DrawCallState(Renderer renderer, int imageCount, ShaderSet shaderSet) {
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
                shaderSet);
    }

    /**
     * Creates a draw call state
     *
     * @param device the device to draw with
     * @param physicalDevice the physical device to draw with
     * @param extent the render bounds of the window
     * @param renderPass the render pass used
     * @param layoutFactory texture descriptor set layout factory
     * @param textureSetFactory texture descriptor set factory
     * @param imageCount number of swapchain images used
     * @param shaderSet the shader set that will be used to draw the objects with
     */
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

    /**
     * Get the list of draw data
     *
     * @return {@code mDrawData.values()}
     */
    public Collection<DrawData> getDrawData() {
        return mDrawData.values();
    }

    /**
     * Start draw data
     *
     * <p>This method will prepare internal data structures for a new frame.
     */
    public void startDrawData() {
        for (DrawData d : mDrawData.values()) d.mObjects.clear();

        mDrawData.entrySet().removeIf(e -> e.getValue().getMesh().getRefCount() <= 0);
    }

    /**
     * Ends draw data
     *
     * @param the index of framebuffer image that's used this frame
     */
    public void endDrawData(int imageIndex) {
        Long descriptorSet =
                mDescriptorPool == null ? null : mDescriptorPool.getDescriptorSet(imageIndex);
        for (DrawData d : mDrawData.values()) d.endDrawData(imageIndex, descriptorSet);
    }

    /**
     * Should this class be removed
     *
     * @return whether this class should be removed. If {@code true}, it has automatically freed
     *     it's data.
     */
    public boolean shouldCleanup() {
        mDrawData.entrySet().removeIf(e -> e.getValue().mObjects.isEmpty());

        if (mDrawData.isEmpty()) {
            free();
        }

        return mDrawData.isEmpty();
    }

    /**
     * Add a renderable to the draw call state
     *
     * @param object object to add
     */
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
            }

            drawData.mMesh = object.getMesh();

            mDrawData.put(new DrawDataHashKey(material, object), drawData);
        }
        drawData.mObjects.add(object);
    }

    /**
     * Update the mesh buffer with new data
     *
     * @param meshBuffer mesh buffer to update
     */
    public void updateMeshBuffer(VulkanMeshBuffer meshBuffer) {
        for (DrawData data : mDrawData.values())
            data.mMeshDescriptor = meshBuffer.addMesh(data.mMesh);
    }

    /**
     * Sets the instance buffer offset
     *
     * <p>Call this method to set an instance buffer offset. It will reserve enough space for itself
     * and then return the next free offset within the buffer.
     *
     * @param offset input offset
     * @return offset + any data this state needs
     */
    public int setInstanceBufferOffset(int offset) {
        for (DrawData d : mDrawData.values())
            offset = d.setInstanceBufferOffset(mShaderSet, offset);
        return offset;
    }

    /**
     * Update the instance buffer with new data
     *
     * @param buffer the instance buffer
     * @param lights lights in the world
     */
    public void updateInstanceBuffer(ByteBuffer buffer, List<Light> lights) {
        for (DrawData d : mDrawData.values()) d.updateInstanceBuffer(mShaderSet, buffer, lights);
    }

    /**
     * Update the instance buffer with new data (slow method)
     *
     * <p>This method is a fallback in case mapping all memory fails
     *
     * @param pData temporary data pointer
     * @param memory instance buffer memory handle
     * @param lights lights in the world
     */
    public void slowUpdateInstanceBuffer(PointerBuffer pData, long memory, List<Light> lights) {
        for (DrawData d : mDrawData.values())
            d.slowUpdateInstanceBuffer(mShaderSet, pData, memory, lights);
    }

    @Override
    public void free() {
        if (mPipeline != null) mPipeline.free();
        mPipeline = null;
    }

    /**
     * Combine the descriptor sets into one long array
     *
     * @param arr previous array. It will be returned out if its length is the same as the number of
     *     non-null entries
     * @param entries the entries that will be put into the array
     * @return all non-null entries as an array
     */
    private static long[] combineDescriptorSets(long[] arr, Long... entries) {
        int elemCount = 0;
        for (Long l : entries) if (l != null) elemCount++;
        long[] ret = (arr != null && elemCount == arr.length) ? arr : new long[elemCount];
        elemCount = 0;
        for (Long l : entries) if (l != null) ret[elemCount++] = l;
        return ret;
    }

    /**
     * Combine the descriptor sets into one long array
     *
     * @param entries the entries that will be put into the array
     * @return all non-null entries as an array
     */
    private static long[] combineDescriptorSetLayouts(Long... layouts) {
        return combineDescriptorSets(null, layouts);
    }
}
