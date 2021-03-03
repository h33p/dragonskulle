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
    public static int OFFSETOF_VIEW = 0;
    public static int OFFSETOF_PROJ = 4 * 4 * 4;

    public Matrix4fc view = new Matrix4f();
    public Matrix4fc proj = new Matrix4f();

    public void copyTo(ByteBuffer buffer, int offset) {
        view.get(OFFSETOF_VIEW + offset, buffer);
        proj.get(OFFSETOF_PROJ + offset, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer, 0);
    }
}
