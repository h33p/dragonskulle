/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.*;

/**
 * Describes a single vertex
 *
 * @author Aurimas Blažulionis
 */
@Builder
@Accessors(prefix = "m")
public class Vertex {
    public static int SIZEOF = (3 + 3 + 2) * 4;
    public static int OFFSETOF_POS = 0;
    public static int OFFSETOF_COL = OFFSETOF_POS + 3 * 4;
    public static int OFFSETOF_UV = OFFSETOF_COL + 3 * 4;

    /** Binding descriptor for vertex buffer */
    public static final BindingDescription BINDING_DESCRIPTION =
            new BindingDescription(0, SIZEOF, VK_VERTEX_INPUT_RATE_VERTEX);

    /** Attribute descriptions for vertex buffer */
    public static final AttributeDescription[] ATTRIBUTE_DESCRIPTIONS = {
        new AttributeDescription(0, 0, VK_FORMAT_R32G32B32_SFLOAT, OFFSETOF_POS),
        new AttributeDescription(0, 1, VK_FORMAT_R32G32B32_SFLOAT, OFFSETOF_COL),
        new AttributeDescription(0, 2, VK_FORMAT_R32G32_SFLOAT, OFFSETOF_UV),
    };

    @Getter private Vector3fc mPos;
    @Getter private Vector3fc mColor;
    @Getter private Vector2fc mUv;

    /** Copy the vertice to a byte buffer */
    public void copyTo(int offset, ByteBuffer buffer) {
        mPos.get(offset + OFFSETOF_POS, buffer);
        mColor.get(offset + OFFSETOF_COL, buffer);
        mUv.get(offset + OFFSETOF_UV, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer.position(), buffer);
    }
}
