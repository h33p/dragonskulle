/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.VulkanPipeline.AttributeDescription;
import org.dragonskulle.renderer.VulkanPipeline.BindingDescription;
import org.lwjgl.system.MemoryStack;

public class UnlitMaterial implements IMaterial {

    private Resource<ShaderBuf> mVertexShader =
            ShaderBuf.getResource("shader", ShaderKind.VERTEX_SHADER);
    private Resource<ShaderBuf> mFragmentShader =
            ShaderBuf.getResource("shader", ShaderKind.FRAGMENT_SHADER);

    /// Vertex shader

    public ShaderBuf getVertexShader() {
        return mVertexShader.get();
    }

    public BindingDescription vertexInstanceBindingDescription() {
        return null;
    }

    public AttributeDescription[] vertexAttributeDescriptions() {
        return null;
    }

    public void writeVertexInstanceData(int offset, ByteBuffer buffer) {}

    // public boolean hasVertexLighting();
    public int vertexUniformDataSize() {
        return 0;
    }

    public boolean isVertexUniformDataDirty() {
        return false;
    }

    public void writeVertexUniformData(int offset, ByteBuffer buffer) {}

    // TODO: Add this
    // public Texture[] getVertexTextures();

    /// TODO: Geometry shader

    /// Fragment shader

    public ShaderBuf getFragmentShader() {
        return mFragmentShader.get();
    }

    public int fragmentPushConstantSize() {
        return 0;
    }

    public ByteBuffer getFragmentPushConstants(MemoryStack stack) {
        return null;
    }

    public int fragmentUniformDataSize() {
        return 0;
    }

    public boolean isFragmentUniformDataDirty() {
        return false;
    }

    public void writeFragmentUniformData(int offset, ByteBuffer buffer) {}

    public boolean hasFragmentTextures() {
        return true;
    }
}
