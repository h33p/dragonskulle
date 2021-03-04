/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.*;

/**
 * Class describing camera projection properties
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Camera extends Component implements ILateFrameUpdate {
    /** Option whether camera is in perspective (3D), or orthographic (2D) mode */
    public static enum Projection {
        ORTHOGRAPHIC,
        PERSPECTIVE
    }

    /** Controls camera projection mode */
    public Projection projection = Projection.PERSPECTIVE;
    /** Controls field of view in perspective mode */
    public float fov = 45.f;
    /** Controls how wide the screen is in orthographic mode */
    public float orthographicSize = 10.f;
    /**
     * Controls how close the nearest objects can be to the camera to render
     *
     * <p>Note the too small value for nearPlane may lead to visual artifacting
     */
    public float nearPlane = 0.1f;
    /**
     * Controls how far the furthest objects can be from the camera to render
     *
     * <p>Note that too large value for farPlane may lead to visual artifacting
     */
    public float farPlane = 100.f;

    /** Current projection matrix */
    private Matrix4f mProj = new Matrix4f();

    @Getter
    /** Current screen aspect ratio */
    private float mAspectRatio = 1.f;

    @Getter
    @Accessors(prefix = "s")
    private static Camera sMainCamera = null;

    /**
     * Get the current projection matrix
     *
     * <p>This method will update the camera's projection matrix and return it back
     */
    public Matrix4f getProj() {
        switch (projection) {
            case PERSPECTIVE:
                mProj.setPerspective(fov, mAspectRatio, nearPlane, farPlane, true);
                break;
            case ORTHOGRAPHIC:
                mProj.setOrtho(
                        -orthographicSize * 0.5f * mAspectRatio,
                        orthographicSize * 0.5f * mAspectRatio,
                        -orthographicSize * 0.5f,
                        orthographicSize * 0.5f,
                        nearPlane,
                        farPlane,
                        true);
                break;
        }
        mProj.scale(1.f, -1.f, 1.f);
        return mProj;
    }

    public void updateAspectRatio(int width, int height) {
        mAspectRatio = (float) width / (float) height;
    }

    public Matrix4fc getView() {
        return getGameObject().getTransform().getWorldMatrix();
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        if (sMainCamera == null) sMainCamera = this;
    }

    @Override
    public void onDestroy() {
        if (sMainCamera == this) sMainCamera = null;
    }
}
