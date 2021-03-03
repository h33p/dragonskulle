/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

/**
 * Class storing meshes in a vertex/index buffer
 *
 * <p>This stores all meshes and provides a way to query
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
class VulkanMeshBuffer implements NativeResource {
    private VkDevice mDevice;
    private PhysicalDevice mPhysicalDevice;
    private VulkanBuffer mVertexBuffer;
    private VulkanBuffer mIndexBuffer;

    private int mMaxVertexOffset;
    private int mMaxIndexOffset;

    @Getter private boolean mDirty = false;
    private Map<Mesh, MeshDescriptor> mLoadedMeshes = new HashMap<Mesh, MeshDescriptor>();

    @Builder
    @Getter
    public static class MeshDescriptor {
        private int mVertexOffset;
        private int mIndexOffset;
        private int mIndexCount;
    }

    private VulkanMeshBuffer() {}

    public VulkanMeshBuffer(VkDevice device, PhysicalDevice physicalDevice) {
        mDevice = device;
        mPhysicalDevice = physicalDevice;
    }

    public long getVertexBuffer() {
        return mVertexBuffer.buffer;
    }

    public long getIndexBuffer() {
        return mIndexBuffer.buffer;
    }

    public MeshDescriptor getMeshDescriptor(Mesh mesh) {
        return mLoadedMeshes.get(mesh);
    }

    public MeshDescriptor addMesh(Mesh mesh) {
        MeshDescriptor ret = mLoadedMeshes.get(mesh);
        if (ret == null) {
            mDirty = true;
            ret = new MeshDescriptor(mMaxVertexOffset, mMaxIndexOffset, mesh.indices.length);
            mMaxVertexOffset += mesh.vertices.length * Vertex.SIZEOF;
            mMaxIndexOffset += mesh.indices.length * 4;
            mLoadedMeshes.put(mesh, ret);
        }
        return ret;
    }

    public VulkanMeshBuffer commitChanges(VkQueue graphicsQueue, long commandPool) {
        if (mDirty) {
            mDirty = false;
            VulkanMeshBuffer ret = new VulkanMeshBuffer();

            ret.mDevice = mDevice;
            ret.mPhysicalDevice = mPhysicalDevice;

            ret.mMaxVertexOffset = mMaxIndexOffset;
            ret.mMaxIndexOffset = mMaxIndexOffset;

            ret.mLoadedMeshes = mLoadedMeshes;
            ret.mVertexBuffer = createVertexBuffer(graphicsQueue, commandPool);
            ret.mIndexBuffer = createIndexBuffer(graphicsQueue, commandPool);

            return ret;
        } else {
            return this;
        }
    }

    // TODO: remove mesh

    @Override
    public void free() {
        if (mVertexBuffer != null) mVertexBuffer.free();
        mVertexBuffer = null;

        if (mIndexBuffer != null) mIndexBuffer.free();
        mIndexBuffer = null;
    }

    /**
     * Create a vertex buffer
     *
     * <p>As the name implies, this buffer holds vertices
     */
    private VulkanBuffer createVertexBuffer(VkQueue graphicsQueue, long commandPool) {
        Renderer.LOGGER.info("Create vertex buffer");

        int vertexCount = 0;

        for (Mesh m : mLoadedMeshes.keySet()) {
            vertexCount += m.vertices.length;
        }

        try (MemoryStack stack = stackPush()) {

            long size = vertexCount * Vertex.SIZEOF;

            try (VulkanBuffer stagingBuffer =
                    new VulkanBuffer(
                            mDevice,
                            mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) {

                PointerBuffer pData = stack.pointers(0);
                vkMapMemory(mDevice, stagingBuffer.memory, 0, size, 0, pData);
                ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
                for (Map.Entry<Mesh, MeshDescriptor> mesh : mLoadedMeshes.entrySet()) {
                    int voff = mesh.getValue().mVertexOffset;
                    for (Vertex v : mesh.getKey().vertices) {
                        v.copyTo(voff, byteBuffer);
                        voff += Vertex.SIZEOF;
                    }
                }
                vkUnmapMemory(mDevice, stagingBuffer.memory);

                VulkanBuffer vertexBuffer =
                        new VulkanBuffer(
                                mDevice,
                                mPhysicalDevice,
                                size,
                                VK_BUFFER_USAGE_TRANSFER_DST_BIT
                                        | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

                VkCommandBuffer commandBuffer =
                        Renderer.beginSingleUseCommandBuffer(mDevice, commandPool);
                stagingBuffer.copyTo(commandBuffer, vertexBuffer, size);
                Renderer.endSingleUseCommandBuffer(
                        commandBuffer, mDevice, graphicsQueue, commandPool);
                return vertexBuffer;
            }
        }
    }

    /**
     * Create index buffer
     *
     * <p>This buffer holds indices of the vertices to render in multiples of 3.
     */
    private VulkanBuffer createIndexBuffer(VkQueue graphicsQueue, long commandPool) {
        Renderer.LOGGER.info("Setup index buffer");

        int indexCount = 0;

        for (Mesh m : mLoadedMeshes.keySet()) {
            indexCount += m.indices.length;
        }

        try (MemoryStack stack = stackPush()) {

            long size = indexCount * 4;

            try (VulkanBuffer stagingBuffer =
                    new VulkanBuffer(
                            mDevice,
                            mPhysicalDevice,
                            size,
                            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) {

                PointerBuffer pData = stack.pointers(0);
                vkMapMemory(mDevice, stagingBuffer.memory, 0, size, 0, pData);
                ByteBuffer byteBuffer = pData.getByteBuffer((int) size);
                for (Map.Entry<Mesh, MeshDescriptor> mesh : mLoadedMeshes.entrySet()) {
                    ByteBuffer buf = byteBuffer.position(mesh.getValue().mIndexOffset);
                    for (int i : mesh.getKey().indices) {
                        buf.putInt(i);
                    }
                }
                vkUnmapMemory(mDevice, stagingBuffer.memory);

                VulkanBuffer indexBuffer =
                        new VulkanBuffer(
                                mDevice,
                                mPhysicalDevice,
                                size,
                                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

                VkCommandBuffer commandBuffer =
                        Renderer.beginSingleUseCommandBuffer(mDevice, commandPool);
                stagingBuffer.copyTo(commandBuffer, indexBuffer, size);
                Renderer.endSingleUseCommandBuffer(
                        commandBuffer, mDevice, graphicsQueue, commandPool);

                return indexBuffer;
            }
        }
    }
}
