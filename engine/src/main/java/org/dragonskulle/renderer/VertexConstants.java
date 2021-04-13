/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import org.joml.*;

/**
 * Constant properties vertex shaders receive
 *
 * <p>These should essentially be universal properties for all shaders, like camera transformation
 *
 * @author Aurimas Blažulionis
 */
class VertexConstants {
    public static int SIZEOF = 4 * 4 * 4 * 2;
    public static int VIEW_OFFSET = 0;
    public static int PROJ_OFFSET = 4 * 4 * 4;

    /** World to view matrix of the camera */
    public Matrix4fc view = new Matrix4f();
    /** Projection matrix of the camera */
    public Matrix4fc proj = new Matrix4f();

    /**
     * Copy the constants to a byte buffer
     *
     * @param buffer the buffer to write the data to
     * @param offset the offset within the buffer
     */
    public void copyTo(ByteBuffer buffer, int offset) {
        view.get(VIEW_OFFSET + offset, buffer);
        proj.get(PROJ_OFFSET + offset, buffer);
    }

    /**
     * Copy the constants to a byte buffer
     *
     * <p>This method will copy the data to the start of the buffer.
     *
     * <p>TODO: streamline this with Vertex, and maybe even remove?
     *
     * @param buffer the buffer to write the data to
     */
    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer, 0);
    }
}
