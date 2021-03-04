/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import lombok.Builder;
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
@Builder
public class Mesh {
    @Getter
    /** Vertices of the mesh */
    private Vertex[] mVertices;

    @Getter
    /** Indices of the mesh. In pairs of 3, forming triangles */
    private int[] mIndices;

    private static final Vertex[] HEXAGON_VERTICES = {
        new Vertex(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(1.0f), new Vector2f(0.0f, 0.0f)),
        new Vertex(
                new Vector3f(-0.5f, 0.86603f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(-0.5f, 0.86603f)),
        new Vertex(
                new Vector3f(0.5f, 0.86603f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(0.5f, 0.86603f)),
        new Vertex(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(1.0f), new Vector2f(1.0f, 0.0f)),
        new Vertex(
                new Vector3f(0.5f, -0.86603f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(0.5f, -0.86603f)),
        new Vertex(
                new Vector3f(-0.5f, -0.86603f, 0.0f),
                new Vector3f(1.0f),
                new Vector2f(-0.5f, -0.86603f)),
        new Vertex(new Vector3f(-1.0f, 0.0f, 0.0f), new Vector3f(1.0f), new Vector2f(-1.0f, 0.0f)),
    };

    private static final int[] HEXAGON_INDICES = {
        1, 0, 2, 2, 0, 3, 3, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 1
    };

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

    /** Standard hexagon mesh */
    public static final Mesh HEXAGON = new Mesh(HEXAGON_VERTICES, HEXAGON_INDICES);

    /** Standard cube mesh */
    public static final Mesh CUBE = new Mesh(CUBE_VERTICES, CUBE_INDICES);

    // TODO: mesh optimization methods, and other utilities
}
