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
 * new class extending {@code ShaderSet}, whose default constructor loads the correct shaders, and
 * sets up their layout properties to be correct.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class ShaderSet implements NativeResource {

    /**
     * Enum describing common render orders
     *
     * <p>Generally, transparent objects should render on top of opaque ones, while UI should be
     * rendered above all.
     */
    public static enum RenderOrder {
        OPAQUE(1000),
        TRANSPARENT(2000),
        UI(10000);

        @Getter private final int mValue;

        private RenderOrder(int value) {
            mValue = value;
        }
    }

    /** Vertex shader used. */
    protected Resource<ShaderBuf> mVertexShader;
    /** Geometry shader used. */
    protected Resource<ShaderBuf> mGeometryShader;
    /** Fragment shader used. */
    protected Resource<ShaderBuf> mFragmentShader;

    /** Controls the order in which the material is rendered. Lower values mean earlier */
    @Getter protected int mRenderOrder = RenderOrder.OPAQUE.getValue();

    /**
     * Controls whether material should perform depth testing when rendering.
     *
     * <p>Generally, transparent and UI elements do not need depth testing.
     */
    @Getter protected boolean mDepthTest = true;

    /** Controls whether the material should do alpha blending (transparency). */
    @Getter protected boolean mAlphaBlend = false;

    /**
     * Constrols whether the material's objects need to be sorted before rendering
     *
     * <p>This option is required for UI elements, and other objects that depend on order being
     * correct for rendering. Presorted objects will be rendered after non-presorted ones, so,
     * OPAQUE pre-sorted object would be rendered after TRANSPARENT non-pre-sorted object.
     */
    @Getter protected boolean mPreSort = false;

    /**
     * Per instance binding description. Not setting will lead to no per-instance data being passed
     */
    @Getter protected BindingDescription mVertexBindingDescription = null;
    /**
     * Per instance data attribute descriptions. This field describes all field layouts that will be
     * passed to the shader.
     */
    @Getter protected AttributeDescription[] mVertexAttributeDescriptions = {};

    /** Shared uniform data size. This is to be passed to all objects using this shader set. */
    @Getter protected int mVertexUniformDataSize = 0;
    // TODO: handle this
    /** Should we update the uniform buffer?. */
    @Getter protected boolean mVertexUniformDataDirty = false;

    /** Shared uniform data size. This is to be passed to all objects using this shader set. */
    @Getter protected int mGeometryUniformDataSize = 0;
    // TODO: handle this
    /** Should we update the uniform buffer?. */
    @Getter protected boolean mGeometryUniformDataDirty = false;

    // TODO: handle push constants
    /** Sets shared constants to be passed to the fragment shader. */
    @Getter protected int mFragmentPushConstantSize = 0;

    /** Shared uniform data size for the fragment shader. */
    @Getter protected int mFragmentUniformDataSize = 0;
    /** Should we update the fragment uniform buffer? */
    @Getter protected boolean mFragmentUniformDataDirty = false;

    /** Number of lights used by the shader. */
    @Getter protected int mLightCount = 0;

    /**
     * Sets how many textures are used by the shader.
     *
     * <p>Failure to set all textures inside the material will result in them having error texture
     * being automatically applied.
     */
    @Getter protected int mNumFragmentTextures = 0;

    /**
     * Retrieve underlying vertex shader.
     *
     * @return vertex shader if set, {@code null} otherwise
     */
    public ShaderBuf getVertexShader() {
        return mVertexShader == null ? null : mVertexShader.get();
    }

    /**
     * Retrieve underlying geometry shader.
     *
     * @return geometry shader if set, {@code null} otherwise
     */
    public ShaderBuf getGeometryShader() {
        return mGeometryShader == null ? null : mGeometryShader.get();
    }

    /**
     * Retrieve underlying fragment shader.
     *
     * @return fragment shader if set, {@code null} otherwise
     */
    public ShaderBuf getFragmentShader() {
        return mFragmentShader == null ? null : mFragmentShader.get();
    }

    /**
     * Write the uniform data used by vertex shader.
     *
     * <p>TODO: actually support this in the renderer
     */
    public void writeVertexUniformData(int offset, ByteBuffer buffer) {}

    /**
     * Write the uniform data used by vertex shader.
     *
     * <p>TODO: actually support this in the renderer
     */
    public void writeGeometryUniformData(int offset, ByteBuffer buffer) {}

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
     * Write the uniform data used by fragment shader.
     *
     * <p>TODO: actually support this in the renderer
     */
    public void writeFragmentUniformData(int offset, ByteBuffer buffer) {}

    /**
     * Retrieve number of uniform bindings used.
     *
     * <p>This essentially checks if vertex/fragment uniform buffers are used and returns according
     * value.
     *
     * @return number of uniform bindings used
     */
    public int numUniformBindings() {
        int numBindings = 0;

        boolean hasFragmentUniform = getFragmentUniformDataSize() > 0;
        numBindings += hasFragmentUniform ? 1 : 0;

        boolean hasGeomUniform = getGeometryUniformDataSize() > 0;
        numBindings += hasGeomUniform ? 1 : 0;

        boolean hasVertexUniform = getVertexUniformDataSize() > 0;
        numBindings += hasVertexUniform ? 1 : 0;

        return numBindings;
    }

    /**
     * Write a matrix to the offset and return the offset after it.
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

    /** Free the underlying vertex and fragment shaders. */
    @Override
    public void free() {
        if (mVertexShader != null) {
            mVertexShader.free();
        }
        mVertexShader = null;

        if (mGeometryShader != null) {
            mGeometryShader.free();
        }
        mGeometryShader = null;

        if (mFragmentShader != null) {
            mFragmentShader.free();
        }
        mFragmentShader = null;
    }
}
