/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import java.nio.ByteBuffer;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderSet;
import org.joml.Matrix4fc;
import org.lwjgl.system.NativeResource;

/**
 * Interface for instances of materials
 *
 * @author Aurimas Blažulionis
 */
public interface IMaterial extends NativeResource {
    /**
     * Gets the shader set of the material. It should be final, and unchanging.
     *
     * @return the shader set containing all shared properties of materials of this type
     */
    ShaderSet getShaderSet();

    /**
     * Write instanced material properties to specified offset
     *
     * @param offset where to write within the byte buffer
     * @param buffer buffer to write into
     * @param matrix transformation matrix of the object. TODO: is this matrix necessary?
     */
    void writeVertexInstanceData(int offset, ByteBuffer buffer, Matrix4fc matrix);

    SampledTexture[] getFragmentTextures();
}
