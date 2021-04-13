/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.joml.*;

/**
 * Describes a single vertex as a read-only view
 *
 * @author Aurimas Blažulionis
 */
public abstract class Vertexc implements Serializable {
    public static int POS_OFFSET = 0;
    public static int NORMAL_OFFSET = POS_OFFSET + 3 * 4;
    public static int COL_OFFSET = NORMAL_OFFSET + 3 * 4;
    public static int UV_OFFSET = COL_OFFSET + 3 * 4;

    public static int SIZEOF = UV_OFFSET + 3 * 4;

    /** Binding descriptor for vertex buffer */
    public static final BindingDescription BINDING_DESCRIPTION =
            new BindingDescription(0, SIZEOF, VK_VERTEX_INPUT_RATE_VERTEX);

    /** Attribute descriptions for vertex buffer */
    public static final AttributeDescription[] ATTRIBUTE_DESCRIPTIONS = {
        new AttributeDescription(0, 0, VK_FORMAT_R32G32B32_SFLOAT, POS_OFFSET),
        new AttributeDescription(0, 1, VK_FORMAT_R32G32B32_SFLOAT, NORMAL_OFFSET),
        new AttributeDescription(0, 2, VK_FORMAT_R32G32B32_SFLOAT, COL_OFFSET),
        new AttributeDescription(0, 3, VK_FORMAT_R32G32_SFLOAT, UV_OFFSET),
    };

    /** @return position of the vertex */
    public abstract Vector3fc getPos();

    /** @return normal vector of the vertex (used for lighting calculations) */
    public abstract Vector3fc getNormal();

    /** @return colour of the vertex */
    public abstract Vector3fc getColor();

    /** @return UV coordinate of the vertex */
    public abstract Vector2fc getUv();

    /**
     * Copy the vertex to a byte buffer
     *
     * @param offset the offset within the byte buffer
     * @param buffer the buffer to write the data to
     */
    public void copyTo(int offset, ByteBuffer buffer) {
        getPos().get(offset + POS_OFFSET, buffer);
        getNormal().get(offset + NORMAL_OFFSET, buffer);
        getColor().get(offset + COL_OFFSET, buffer);
        getUv().get(offset + UV_OFFSET, buffer);
    }

    /**
     * Copy the vertex to a byte buffer
     *
     * <p>This method will copy the vertex to the buffer at buffer's position, it will not move the
     * position, though
     *
     * @param buffer the buffer to write the data to
     */
    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer.position(), buffer);
    }
}
