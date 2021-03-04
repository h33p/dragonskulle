/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

/**
 * Class describing shared properties of a material shader
 *
 * <p>This class describes various shared properties like shaders used, their data layout, and so
 * on.
 *
 * <p>Check {@code UnlitMaterial} how this class is used, but essentially you would want to create a
 * new class extending {@class ShaderSet}, whose default constructor loads the correct shaders, and
 * sets up their layout properties to be correct.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class ShaderSet implements NativeResource {
    /** Vertex shader used */
    protected Resource<ShaderBuf> mVertexShader;
    /** Fragment shader used */
    protected Resource<ShaderBuf> mFragmentShader;
    // TODO: Geometry shaders

    /**
     * Per instance binding description. Not setting will lead to no per-instance data being passed
     */
    @Getter protected BindingDescription mVertexBindingDescription = null;
    /**
     * Per instance data attribute descriptions. This field describes all field layouts that will be
     * passed to the shader.
     */
    @Getter protected AttributeDescription[] mVertexAttributeDescriptions = {};

    // TODO: handle this
    /** Shared uniform data size. This is to be passed to all objects using this shader set. */
    @Getter protected int mVertexUniformDataSize = 0;
    /** Should we update the uniform buffer? */
    @Getter protected boolean mVertexUniformDataDirty = false;

    // TODO: handle push constants
    /** Sets shared constants to be passed to the fragment shader */
    @Getter protected int mFragmentPushConstantSize = 0;

    /** Shared uniform data size for the fragment shader. */
    @Getter protected int mFragmentUniformDataSize = 0;
    /** Should we update the fragment uniform buffer? */
    @Getter protected boolean mFragmentUniformDataDirty = false;

    /**
     * Sets how many textures are used by the shader.
     *
     * <p>Failure to set all textures inside the material will result in them having error texture
     * being automatically applied.
     */
    @Getter protected int mNumFragmentTextures = 0;

    /**
     * Retrieve underlying vertex shader
     *
     * @return vertex shader if set, {@code null} otherwise
     */
    public ShaderBuf getVertexShader() {
        return mVertexShader == null ? null : mVertexShader.get();
    }

    /**
     * Retrieve underlying fragment shader
     *
     * @return fragment shader if set, {@code null} otherwise
     */
    public ShaderBuf getFragmentShader() {
        return mFragmentShader == null ? null : mFragmentShader.get();
    }

    /**
     * Write the uniform data used by vertex shader
     *
     * <p>TODO: actually support this in the renderer
     */
    public void writeVertexUniformData(int offset, ByteBuffer buffer) {}

    /**
     * Get the push constants used by fragment shader
     *
     * <p>Currently using this feature is unsupported.
     *
     * <p>TODO: actually support this in the renderer
     */
    public ByteBuffer getFragmentPushConstants(MemoryStack stack) {
        return null;
    }

    /**
     * Write the uniform data used by fragment shader
     *
     * <p>TODO: actually support this in the renderer
     */
    public void writeFragmentUniformData(int offset, ByteBuffer buffer) {}

    /**
     * Retrieve number of uniform bindings used
     *
     * <p>This essentially checks if vertex/fragment uniform buffers are used and returns according
     * value
     *
     * @return number of uniform bindings used
     */
    public int numUniformBindings() {
        int numBindings = 0;

        boolean hasFragmentUniform = getFragmentUniformDataSize() > 0;
        numBindings += hasFragmentUniform ? 1 : 0;

        boolean hasVertexUniform = getVertexUniformDataSize() > 0;
        numBindings += hasVertexUniform ? 1 : 0;

        return numBindings;
    }

    /**
     * Write a matrix to the offset and return the offset after it
     *
     * @param offset target offset to write into
     * @param buffer byte buffer to write into
     * @param matrix matrix to write to the buffer
     * @return new offset after this matrix
     */
    public static int writeMatrix(int offset, ByteBuffer buffer, Matrix4fc matrix) {
        matrix.get(offset, buffer);
        return offset + AttributeDescription.MATRIX_SIZE;
    }

    /** Free the underlying vertex and fragment shaders */
    @Override
    public void free() {
        if (mVertexShader != null) mVertexShader.free();
        mVertexShader = null;

        if (mFragmentShader != null) mFragmentShader.free();
        mFragmentShader = null;
    }
}
