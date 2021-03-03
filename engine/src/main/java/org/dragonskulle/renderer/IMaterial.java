/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import org.joml.Matrix4fc;
import org.lwjgl.system.NativeResource;

public interface IMaterial extends NativeResource {
    public ShaderSet getShaderSet();

    public void writeVertexInstanceData(int offset, ByteBuffer buffer, Matrix4fc matrix);

    public SampledTexture[] getFragmentTextures();
}
