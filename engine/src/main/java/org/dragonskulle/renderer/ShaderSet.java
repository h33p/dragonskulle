/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.VulkanPipeline.AttributeDescription;
import org.dragonskulle.renderer.VulkanPipeline.BindingDescription;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

@Accessors(prefix = "m")
public class ShaderSet implements NativeResource {
    protected Resource<ShaderBuf> mVertexShader;
    protected Resource<ShaderBuf> mFragmentShader;
    // TODO: Geometry shaders

    @Getter protected BindingDescription mVertexBindingDescription = null;
    @Getter protected AttributeDescription[] mVertexAttributeDescriptions = {};

    @Getter protected int mVertexUniformDataSize = 0;
    @Getter protected boolean mVertexUniformDataDirty = false;

    @Getter protected int mFragmentPushConstantSize = 0;

    @Getter protected int mFragmentUniformDataSize = 0;
    @Getter protected boolean mFragmentUniformDataDirty = false;

    @Getter protected int mNumFragmentTextures = 0;

    public ByteBuffer getFragmentPushConstants(MemoryStack stack) {
        return null;
    }

    public void writeFragmentUniformData(int offset, ByteBuffer buffer) {}

    public ShaderBuf getVertexShader() {
        return mVertexShader == null ? null : mVertexShader.get();
    }

    public ShaderBuf getFragmentShader() {
        return mFragmentShader == null ? null : mFragmentShader.get();
    }

    public int numUniformBindings() {
        int numBindings = 0;

        boolean hasFragmentUniform = getFragmentUniformDataSize() > 0;
        numBindings += hasFragmentUniform ? 1 : 0;

        boolean hasVertexUniform = getVertexUniformDataSize() > 0;
        numBindings += hasVertexUniform ? 1 : 0;

        return numBindings;
    }

    public static final int MATRIX_SIZE = 4 * 4 * 4;

    public static int writeMatrix(int offset, ByteBuffer buffer, Matrix4fc matrix) {
        matrix.get(offset, buffer);
        return offset + MATRIX_SIZE;
    }

    @Override
    public void free() {
        if (mVertexShader != null) mVertexShader.free();
        if (mFragmentShader != null) mFragmentShader.free();
    }
}

/*
 * List<Renderer> -> Map<(ShaderSet, Mesh, Texture[]), List<(Transform, MaterialProps)>>;
 * Map<DrawData, List<InstancedData>>
 *
 *
 */
