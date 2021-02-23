/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import lombok.Builder;
import org.joml.*;

/**
 * Properties vertex shaders receive
 *
 * @author Aurimas Blažulionis
 */
@Builder
class UniformBufferObject {
    public static int SIZEOF = 4 * 4 * 4 * 3;
    public static int OFFSETOF_MODEL = 0;
    public static int OFFSETOF_VIEW = 4 * 4 * 4;
    public static int OFFSETOF_PROJ = 4 * 4 * 4 * 2;

    public Matrix4f model;
    public Matrix4f view;
    public Matrix4f proj;

    public void copyTo(ByteBuffer buffer) {
        model.get(OFFSETOF_MODEL, buffer);
        view.get(OFFSETOF_VIEW, buffer);
        proj.get(OFFSETOF_PROJ, buffer);
    }
}
