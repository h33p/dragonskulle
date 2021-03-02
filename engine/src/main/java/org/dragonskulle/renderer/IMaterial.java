/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import org.dragonskulle.renderer.VulkanPipeline.AttributeDescription;
import org.dragonskulle.renderer.VulkanPipeline.BindingDescription;
import org.lwjgl.system.MemoryStack;

public interface IMaterial {

    // TODO: implement this properly
    /*public static class SpecializationEntry {
        int constantID;
        int offset;
        int size;
    }*/

    /// Vertex shader

    public ShaderBuf getVertexShader();

    public BindingDescription vertexInstanceBindingDescription();

    public AttributeDescription[] vertexAttributeDescriptions();

    // TODO: We need to somehow allow allocating this
    public void writeVertexInstanceData(int offset, ByteBuffer buffer);

    // public boolean hasVertexLighting();
    public int vertexUniformDataSize();

    public boolean isVertexUniformDataDirty();

    public void writeVertexUniformData(int offset, ByteBuffer buffer);

    // TODO: Add this
    // public Texture[] getVertexTextures();

    /// TODO: Geometry shader

    /// Fragment shader

    public ShaderBuf getFragmentShader();

    public int fragmentPushConstantSize();

    public ByteBuffer getFragmentPushConstants(MemoryStack stack);

    // public boolean hasFragmentLighting();
    public int fragmentUniformDataSize();

    public boolean isFragmentUniformDataDirty();
    // TODO: Potentially pass texture IDs?
    public void writeFragmentUniformData(int offset, ByteBuffer buffer);

    public int numFragmentTextures();
}
