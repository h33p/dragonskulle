/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

public class Camera {

    public static enum Projection {
        ORTHOGRAPHIC,
        PERSPECTIVE
    }

    public Projection projection;
    public float fov;
    public float orthographicSize;
}
