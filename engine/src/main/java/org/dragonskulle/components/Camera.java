/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Reference;
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
    public float fov = 70.f;
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

    private Vector3f mTmpTranslation = new Vector3f();
    private Matrix4f mToView = new Matrix4f();

    private static Reference<Camera> sMainCamera = null;

    /**
     * Access the main camera
     *
     * @return current main camera. Should not be stored for long duration.
     */
    public static Camera getMainCamera() {
        return sMainCamera == null ? null : sMainCamera.get();
    }

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

        mProj.scale(1.f, -1.f, 1.f).lookAt(0.f, 0.f, 0.f, 0.f, nearPlane, 0f, 0.f, 0.f, 1.f);

        return mProj;
    }

    /**
     * Update the screen's aspect ratio
     *
     * @param width current screen width
     * @param height current screen height
     */
    public void updateAspectRatio(int width, int height) {
        mAspectRatio = (float) width / (float) height;
    }

    /**
     * Get world to view transformation matrix
     *
     * @return world to view space transformation matrix
     */
    public Matrix4fc getView() {
        Matrix4fc worldMatrix = getGameObject().getTransform().getWorldMatrix();
        return mToView.set(worldMatrix)
                .setTranslation(worldMatrix.getTranslation(mTmpTranslation).mul(-1.f));
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        if (sMainCamera == null) sMainCamera = getReference(Camera.class);
    }

    @Override
    public void onDestroy() {
        if (getMainCamera() == this) sMainCamera = null;
    }
}
