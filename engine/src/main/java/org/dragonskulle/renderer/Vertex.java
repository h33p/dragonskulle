/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
public class Vertex implements Serializable {
    public static int SIZEOF = (3 + 3 + 2) * 4;
    public static int POS_OFFSET = 0;
    public static int COL_OFFSET = POS_OFFSET + 3 * 4;
    public static int UV_OFFSET = COL_OFFSET + 3 * 4;

    /** Binding descriptor for vertex buffer */
    public static final BindingDescription BINDING_DESCRIPTION =
            new BindingDescription(0, SIZEOF, VK_VERTEX_INPUT_RATE_VERTEX);

    /** Attribute descriptions for vertex buffer */
    public static final AttributeDescription[] ATTRIBUTE_DESCRIPTIONS = {
        new AttributeDescription(0, 0, VK_FORMAT_R32G32B32_SFLOAT, POS_OFFSET),
        new AttributeDescription(0, 1, VK_FORMAT_R32G32B32_SFLOAT, COL_OFFSET),
        new AttributeDescription(0, 2, VK_FORMAT_R32G32_SFLOAT, UV_OFFSET),
    };

    @Getter private Vector3fc mPos;
    @Getter private Vector3fc mColor;
    @Getter private Vector2fc mUv;

    /** Copy the vertice to a byte buffer */
    public void copyTo(int offset, ByteBuffer buffer) {
        mPos.get(offset + POS_OFFSET, buffer);
        mColor.get(offset + COL_OFFSET, buffer);
        mUv.get(offset + UV_OFFSET, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer.position(), buffer);
    }
}
