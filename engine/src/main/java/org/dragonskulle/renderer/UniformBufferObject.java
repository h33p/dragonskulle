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
    public static int SIZEOF = 4 * 4 * 4;
    public static int OFFSETOF_MODEL = 0;

    public Matrix4f model;

    public void copyTo(ByteBuffer buffer, int offset) {
        model.get(OFFSETOF_MODEL + offset, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer, 0);
    }
}
