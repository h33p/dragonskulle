/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.*;

/**
 * Class storing meshes in a vertex/index buffer
 *
 * <p>This stores all meshes and provides a way to query
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Mesh implements Serializable {
    @Getter
    /** Vertices of the mesh */
    private Vertex[] mVertices;

    @Getter
    /** Indices of the mesh. In pairs of 3, forming triangles */
    private int[] mIndices;

    /** Reference count of the mesh used for resource tracking */
    @Getter private int mRefCount = 0;

    private int mCachedHashCode = 0;

    private static final Vertex[] HEXAGON_VERTICES = {
        new Vertex(
                new Vector3f(0.86603f, -0.5f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(0.86603f, -0.5f)),
        new Vertex(
                new Vector3f(0.86603f, 0.5f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(0.86603f, 0.5f)),
        new Vertex(new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(1.0f), new Vector2f(0.0f, 1.0f)),
        new Vertex(
                new Vector3f(-0.86603f, 0.5f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(-0.86603f, 0.5f)),
        new Vertex(
                new Vector3f(-0.86603f, -0.5f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(-0.86603f, -0.5f)),
        new Vertex(new Vector3f(0.0f, -1.0f, 0.0f), new Vector3f(1.0f), new Vector2f(0.0f, -1.0f)),
    };

    private static final int[] HEXAGON_INDICES = {0, 4, 5, 1, 2, 3, 0, 1, 3, 0, 3, 4};

    private static final Vertex[] CUBE_VERTICES = {
        new Vertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(1.f), new Vector2f(0.0f, 0.0f)),
        new Vertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector3f(1.f), new Vector2f(1.0f, 0.0f)),
        new Vertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector3f(1.f), new Vector2f(1.0f, 1.0f)),
        new Vertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector3f(1.f), new Vector2f(0.0f, 1.0f)),
        new Vertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector3f(1.f), new Vector2f(0.0f, 1.0f)),
        new Vertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector3f(1.f), new Vector2f(1.0f, 1.0f)),
        new Vertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(1.f), new Vector2f(1.0f, 0.0f)),
        new Vertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector3f(1.f), new Vector2f(0.0f, 0.0f))
    };

    private static final int[] CUBE_INDICES = {
        0, 2, 1,
        0, 3, 2,
        1, 2, 6,
        6, 5, 1,
        4, 5, 6,
        6, 7, 4,
        2, 3, 6,
        6, 3, 7,
        0, 7, 3,
        0, 4, 7,
        0, 1, 5,
        0, 5, 4
    };

    private static final Vertex[] QUAD_VERTICES = {
        new Vertex(new Vector3f(0f, 0f, 0.f), new Vector3f(1f), new Vector2f(0.f, 0.f)),
        new Vertex(new Vector3f(0f, 1.f, 0.f), new Vector3f(1f), new Vector2f(0f, 1f)),
        new Vertex(new Vector3f(1.f, 0f, 0.f), new Vector3f(1f), new Vector2f(1f, 0f)),
        new Vertex(new Vector3f(1.f, 1.f, 0.f), new Vector3f(1f), new Vector2f(1f, 1f)),
    };

    private static final int[] QUAD_INDICES = {
        0, 1, 2,
        1, 3, 2
    };

    /** Standard hexagon mesh */
    public static final Mesh HEXAGON = new Mesh(HEXAGON_VERTICES, HEXAGON_INDICES);

    /** Standard cube mesh */
    public static final Mesh CUBE = new Mesh(CUBE_VERTICES, CUBE_INDICES);

    /** Standard quad mesh */
    public static final Mesh QUAD = new Mesh(QUAD_VERTICES, QUAD_INDICES);

    public Mesh(Vertex[] vertices, int[] indices) {
        mVertices = vertices;
        mIndices = indices;
    }

    public static void addQuadToList(
            List<Vertex> vertices,
            List<Integer> indices,
            Vector2fc startCoords,
            Vector2fc endCoords,
            Vector2fc startUV,
            Vector2fc endUV) {
        int start = vertices.size();

        indices.add(start);
        indices.add(start + 1);
        indices.add(start + 2);
        indices.add(start + 1);
        indices.add(start + 3);
        indices.add(start + 2);

        vertices.add(
                new Vertex(
                        new Vector3f(startCoords.x(), startCoords.y(), 0.f),
                        new Vector3f(1f),
                        new Vector2f(startUV)));
        vertices.add(
                new Vertex(
                        new Vector3f(startCoords.x(), endCoords.y(), 0.f),
                        new Vector3f(1f),
                        new Vector2f(startUV.x(), endUV.y())));
        vertices.add(
                new Vertex(
                        new Vector3f(endCoords.x(), startCoords.y(), 0.f),
                        new Vector3f(1f),
                        new Vector2f(endUV.x(), startUV.y())));
        vertices.add(
                new Vertex(
                        new Vector3f(endCoords.x(), endCoords.y(), 0.f),
                        new Vector3f(1f),
                        new Vector2f(endUV)));
    }

    public static Mesh buildQuad(
            Vector2fc startCoords, Vector2fc endCoords, Vector2fc startUV, Vector2fc endUV) {
        final Vertex[] vertices = {
            new Vertex(
                    new Vector3f(startCoords.x(), startCoords.y(), 0.f), new Vector3f(1f), startUV),
            new Vertex(
                    new Vector3f(startCoords.x(), endCoords.y(), 0.f),
                    new Vector3f(1f),
                    new Vector2f(startUV.x(), endUV.y())),
            new Vertex(
                    new Vector3f(endCoords.x(), startCoords.y(), 0.f),
                    new Vector3f(1f),
                    new Vector2f(endUV.x(), startUV.y())),
            new Vertex(new Vector3f(endCoords.x(), endCoords.y(), 0.f), new Vector3f(1f), endUV),
        };
        return new Mesh(vertices, QUAD_INDICES);
    }

    // TODO: mesh optimization methods, and other utilities

    public void incRefCount() {
        mRefCount++;
    }

    public void decRefCount() {
        mRefCount--;
    }

    @Override
    public int hashCode() {
        if (mCachedHashCode == 0)
            mCachedHashCode = Objects.hash(Arrays.hashCode(mVertices), Arrays.hashCode(mIndices));
        return mCachedHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Mesh)) return false;
        if (o.hashCode() != hashCode()) return false;
        Mesh mesh = (Mesh) o;
        // There is a potential chance for collision, yes,
        // but that is extremely unlikely. Billions to one!
        return mVertices.length == mesh.mVertices.length && mIndices.length == mesh.mIndices.length;
    }
}
