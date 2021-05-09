/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * Class storing meshes in a vertex/index buffer.
 *
 * <p>This stores all meshes and provides a way to query
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Mesh {
    /** Vertices of the mesh. */
    @Getter private Vertexc[] mVertices;

    /** Indices of the mesh. In pairs of 3, forming triangles. */
    @Getter private int[] mIndices;

    /** The minimum coordinate of the bounding box. */
    private final Vector3f mBBMin = new Vector3f();
    /** The maximum coordinate of the bounding box. */
    private final Vector3f mBBMax = new Vector3f();
    /** The center of the bounding box. */
    private final Vector3f mBBCenter = new Vector3f();

    /** Reference count of the mesh used for resource tracking. */
    @Getter private int mRefCount = 0;

    /**
     * Cached hash code this hashcode is cached so that there is no need to recalculate it every
     * time the method is called.
     */
    private int mCachedHashCode = 0;

    /** Vertices for a 2D hexagon. */
    private static final Vertexc[] HEXAGON_VERTICES = {
        new Vertex(new Vector3f(0.86603f, -0.5f, 0.0f), new Vector2f(0.86603f, -0.5f)),
        new Vertex(new Vector3f(0.86603f, 0.5f, 0.0f), new Vector2f(0.86603f, 0.5f)),
        new Vertex(new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(0.0f, 1.0f)),
        new Vertex(new Vector3f(-0.86603f, 0.5f, 0.0f), new Vector2f(-0.86603f, 0.5f)),
        new Vertex(new Vector3f(-0.86603f, -0.5f, 0.0f), new Vector2f(-0.86603f, -0.5f)),
        new Vertex(new Vector3f(0.0f, -1.0f, 0.0f), new Vector2f(0.0f, -1.0f)),
    };

    /** Indices for the 2D hexagon. */
    private static final int[] HEXAGON_INDICES = {0, 4, 5, 1, 2, 3, 0, 1, 3, 0, 3, 4};

    /** Vertices for a cube. */
    private static final Vertexc[] CUBE_VERTICES = {
        new Vertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 0.0f)),
        new Vertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector2f(1.0f, 0.0f)),
        new Vertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
        new Vertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
        new Vertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector2f(0.0f, 1.0f)),
        new Vertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector2f(1.0f, 1.0f)),
        new Vertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f)),
        new Vertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector2f(0.0f, 0.0f))
    };

    /** Indices for the cube. */
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

    /** Vertices for a 2D quad plane. */
    private static final Vertexc[] QUAD_VERTICES = {
        new Vertex(new Vector3f(0f, 0f, 0.f), new Vector2f(0.f, 0.f)),
        new Vertex(new Vector3f(0f, 1.f, 0.f), new Vector2f(0f, 1f)),
        new Vertex(new Vector3f(1.f, 0f, 0.f), new Vector2f(1f, 0f)),
        new Vertex(new Vector3f(1.f, 1.f, 0.f), new Vector2f(1f, 1f)),
    };

    /** Indices for the 2D quad. */
    private static final int[] QUAD_INDICES = {
        0, 1, 2,
        1, 3, 2
    };

    /** Standard hexagon mesh. */
    public static final Mesh HEXAGON = new Mesh(HEXAGON_VERTICES, HEXAGON_INDICES);

    /** Standard cube mesh. */
    public static final Mesh CUBE = new Mesh(CUBE_VERTICES, CUBE_INDICES);

    /** Standard quad mesh. */
    public static final Mesh QUAD = new Mesh(QUAD_VERTICES, QUAD_INDICES);

    /**
     * Create a mesh with vertices and indices.
     *
     * @param vertices vertices of the mesh
     * @param indices indices of the mesh
     */
    public Mesh(Vertexc[] vertices, int[] indices) {
        mVertices = vertices;
        mIndices = indices;
        calculateBoundingBox();
    }

    /** Get the bounding box minimum. */
    public Vector3fc getBBMin() {
        return mBBMin;
    }

    /** Get the bounding box maximum. */
    public Vector3fc getBBMax() {
        return mBBMax;
    }

    /** Get the bounding box center. */
    public Vector3fc getBBCenter() {
        return mBBCenter;
    }

    /**
     * Builds and appends a quad to a list of vertices and indices.
     *
     * @param vertices the vertices to add the quad to
     * @param indices the indices to add the quad to
     * @param startCoords starting coordinates where to place the quad
     * @param endCoords ending coordinates where to place the quad
     * @param startUV UV coordinates at startCoords
     * @param endUV UV coordinates and endCoords
     */
    public static void addQuadToList(
            List<Vertex> vertices,
            List<Integer> indices,
            Vector2fc startCoords,
            Vector2fc endCoords,
            Vector2fc startUV,
            Vector2fc endUV) {
        addQuadToList(vertices, indices, startCoords, endCoords, startUV, endUV, new Vector4f(1f));
    }

    public static void addQuadToList(
            List<Vertex> vertices,
            List<Integer> indices,
            Vector2fc startCoords,
            Vector2fc endCoords,
            Vector2fc startUV,
            Vector2fc endUV,
            Vector4f colour) {
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
                        colour,
                        new Vector2f(startUV)));
        vertices.add(
                new Vertex(
                        new Vector3f(startCoords.x(), endCoords.y(), 0.f),
                        colour,
                        new Vector2f(startUV.x(), endUV.y())));
        vertices.add(
                new Vertex(
                        new Vector3f(endCoords.x(), startCoords.y(), 0.f),
                        colour,
                        new Vector2f(endUV.x(), startUV.y())));
        vertices.add(
                new Vertex(
                        new Vector3f(endCoords.x(), endCoords.y(), 0.f),
                        colour,
                        new Vector2f(endUV)));
    }

    /**
     * Build a quad mesh with specified coordinates.
     *
     * @param startCoords starting coordinates where to place the quad
     * @param endCoords ending coordinates where to place the quad
     * @param startUV UV coordinates at startCoords
     * @param endUV UV coordinates and endCoords
     */
    public static Mesh buildQuad(
            Vector2fc startCoords, Vector2fc endCoords, Vector2fc startUV, Vector2fc endUV) {
        final Vertex[] vertices = {
            new Vertex(
                    new Vector3f(startCoords.x(), startCoords.y(), 0.f), new Vector4f(1f), startUV),
            new Vertex(
                    new Vector3f(startCoords.x(), endCoords.y(), 0.f),
                    new Vector4f(1f),
                    new Vector2f(startUV.x(), endUV.y())),
            new Vertex(
                    new Vector3f(endCoords.x(), startCoords.y(), 0.f),
                    new Vector4f(1f),
                    new Vector2f(endUV.x(), startUV.y())),
            new Vertex(new Vector3f(endCoords.x(), endCoords.y(), 0.f), new Vector4f(1f), endUV),
        };
        return new Mesh(vertices, QUAD_INDICES);
    }

    // TODO: mesh optimization methods, and other utilities

    /** Increase the reference count of the mesh. */
    public void incRefCount() {
        mRefCount++;
    }

    /** Decrease the reference count of the mesh. */
    public void decRefCount() {
        mRefCount--;
    }

    @Override
    public int hashCode() {
        if (mCachedHashCode == 0) {
            mCachedHashCode = Objects.hash(Arrays.hashCode(mVertices), Arrays.hashCode(mIndices));
        }
        return mCachedHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Mesh)) {
            return false;
        }
        if (o.hashCode() != hashCode()) {
            return false;
        }
        Mesh mesh = (Mesh) o;
        // There is a potential chance for collision, yes,
        // but that is extremely unlikely. Billions to one!
        return mVertices.length == mesh.mVertices.length && mIndices.length == mesh.mIndices.length;
    }

    private void calculateBoundingBox() {
        if (mVertices.length == 0) {
            mBBMin.set(0);
            mBBMax.set(0);
            mBBCenter.set(0);
            return;
        }

        mBBMin.set(Float.POSITIVE_INFINITY);
        mBBMax.set(Float.NEGATIVE_INFINITY);

        for (Vertexc v : mVertices) {
            mBBMin.min(v.getPos());
            mBBMax.max(v.getPos());
        }

        mBBCenter.set(mBBMin).add(mBBMax).mul(0.5f);
    }
}
