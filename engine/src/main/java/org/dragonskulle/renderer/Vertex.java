/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Describes a single vertex.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@EqualsAndHashCode
public class Vertex implements Serializable {
    public static final int POS_OFFSET = 0;
    public static final int NORMAL_OFFSET = POS_OFFSET + 3 * 4;
    public static final int COL_OFFSET = NORMAL_OFFSET + 3 * 4;
    public static final int UV_OFFSET = COL_OFFSET + 3 * 4;

    public static final int SIZEOF = UV_OFFSET + 3 * 4;

    /** Binding descriptor for vertex buffer. */
    public static final BindingDescription BINDING_DESCRIPTION =
            new BindingDescription(0, SIZEOF, VK_VERTEX_INPUT_RATE_VERTEX);

    /** Attribute descriptions for vertex buffer. */
    public static final AttributeDescription[] ATTRIBUTE_DESCRIPTIONS = {
        new AttributeDescription(0, 0, VK_FORMAT_R32G32B32_SFLOAT, POS_OFFSET),
        new AttributeDescription(0, 1, VK_FORMAT_R32G32B32_SFLOAT, NORMAL_OFFSET),
        new AttributeDescription(0, 2, VK_FORMAT_R32G32B32_SFLOAT, COL_OFFSET),
        new AttributeDescription(0, 3, VK_FORMAT_R32G32_SFLOAT, UV_OFFSET),
    };

    @Getter @Setter private Vector3fc mPos;
    @Getter @Setter private Vector3fc mNormal;
    @Getter @Setter private Vector3fc mColor;
    @Getter @Setter private Vector2fc mUv;

    public Vertex() {
        this(new Vector3f(), new Vector3f(0f, 0f, 1f), new Vector3f(1f), new Vector2f());
    }

    public Vertex(Vector3fc pos, Vector3fc color, Vector2fc uv) {
        this(pos, new Vector3f(0f, 0f, 1f), color, uv);
    }

    public Vertex(Vector3fc pos, Vector3fc normal, Vector3fc color, Vector2fc uv) {
        mPos = pos;
        mNormal = normal;
        mColor = color;
        mUv = uv;
    }

    /** Copy the vertice to a byte buffer. */
    public void copyTo(int offset, ByteBuffer buffer) {
        mPos.get(offset + POS_OFFSET, buffer);
        mNormal.get(offset + NORMAL_OFFSET, buffer);
        mColor.get(offset + COL_OFFSET, buffer);
        mUv.get(offset + UV_OFFSET, buffer);
    }

    public void copyTo(ByteBuffer buffer) {
        copyTo(buffer.position(), buffer);
    }
}
