/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Describes a single vertex.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@EqualsAndHashCode
public class Vertex extends Vertexc {

    /** Position of the vertex. */
    @Getter private Vector3f mPos = new Vector3f();
    /** Normal vector of the vertex (used for lighting calculations). */
    @Getter private Vector3f mNormal = new Vector3f(0f, 0f, 1f);
    /** Colour of the vertex. */
    @Getter private Vector4f mColor = new Vector4f(1f);
    /** UV coordinate of the vertex. */
    @Getter private Vector2f mUv = new Vector2f();

    /** Create a default vertex. */
    public Vertex() {}

    /**
     * Create a vertex.
     *
     * @param pos position of the vertex
     * @param color colour of the vertex
     * @param uv UV texture coordinate of the vertex
     */
    public Vertex(Vector3fc pos, Vector4fc color, Vector2fc uv) {
        this(pos, new Vector3f(0f, 0f, 1f), color, uv);
    }

    /**
     * Create a vertex.
     *
     * @param pos position of the vertex
     * @param normal normal vector of the vertex
     * @param color colour of the vertex
     * @param uv UV texture coordinate of the vertex
     */
    public Vertex(Vector3fc pos, Vector3fc normal, Vector4fc color, Vector2fc uv) {
        mPos.set(pos);
        mNormal.set(normal);
        mColor.set(color);
        mUv.set(uv);
    }

    /**
     * Create a vertex from position and UV coordinates.
     *
     * <p>This vertex will have normals pointing up.
     *
     * @param pos position of the vertex.
     * @param uv UV texture coordinate of the vertex.
     */
    public Vertex(Vector3fc pos, Vector2fc uv) {
        this(pos, new Vector3f(0f, 0f, 1f), new Vector4f(1f), uv);
    }
}
