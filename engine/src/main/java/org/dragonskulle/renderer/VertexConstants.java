/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Constant properties vertex shaders receive.
 *
 * <p>These should essentially be universal properties for all shaders, like camera transformation
 *
 * @author Aurimas Blažulionis
 */
class VertexConstants {
    public static final int SIZEOF = 4 * 4 * 4 * 2;
    public static final int VIEW_OFFSET = 0;
    public static final int PROJ_OFFSET = 4 * 4 * 4;

    public Matrix4fc mView = new Matrix4f();
    public Matrix4fc mProj = new Matrix4f();

    public void copyTo(ByteBuffer buffer, int offset) {
        mView.get(VIEW_OFFSET + offset, buffer);
        mProj.get(PROJ_OFFSET + offset, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer, 0);
    }
}
