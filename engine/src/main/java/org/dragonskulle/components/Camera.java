/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.*;

@Accessors(prefix = "m")
public class Camera {
    public static enum Projection {
        ORTHOGRAPHIC,
        PERSPECTIVE
    }

    public Projection projection = Projection.PERSPECTIVE;
    public float fov = 45.f;
    public float orthographicSize = 100.f;
    public float nearPlane = 0.1f;
    public float farPlane = 100.f;

    @Getter Matrix4f mProj = new Matrix4f();

    // TODO: remove this, it's temporary
    @Getter
    Matrix4f mView =
            new Matrix4f()
                    .lookAt(
                            new Vector3f(2.0f, 2.0f, 2.0f),
                            new Vector3f(0.0f, 0.0f, -0.05f),
                            new Vector3f(0.0f, 0.0f, 1.0f));

    public void updateProjection(int width, int height) {
        mProj.setPerspective(fov, (float) width / (float) height, nearPlane, farPlane, true);
        mProj.m11(-mProj.m11());
    }
}
