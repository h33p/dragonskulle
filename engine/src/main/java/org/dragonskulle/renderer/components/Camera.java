/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.components;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.Transform;
import org.dragonskulle.core.Scene;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Class describing camera projection properties.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Camera extends Component implements IFrameUpdate {
    /** Define which way is up. */
    private static final Vector3f UP_DIR = new Vector3f(0f, 0f, 1f);

    /** Option whether camera is in perspective (3D), or orthographic (2D) mode. */
    public static enum Projection {
        ORTHOGRAPHIC,
        PERSPECTIVE
    }

    /** Controls camera projection mode. */
    @Getter @Setter private Projection mProjection = Projection.PERSPECTIVE;
    /** Controls field of view in perspective mode. */
    @Getter @Setter private float mFov = 70.f;
    /** Controls how wide the screen is in orthographic mode. */
    @Getter @Setter private float mOrthographicSize = 10.f;
    /**
     * Controls how close the nearest objects can be to the camera to render.
     *
     * <p>Note the too small value for nearPlane may lead to visual artifacting.
     */
    @Getter @Setter private float mNearPlane = 0.1f;
    /**
     * Controls how far the furthest objects can be from the camera to render.
     *
     * <p>Note that too large value for farPlane may lead to visual artifacting.
     */
    @Getter @Setter private float mFarPlane = 100.f;

    /** Current projection matrix. */
    private Matrix4f mProj = new Matrix4f();

    @Getter
    /* Current screen aspect ratio. */
    private float mAspectRatio = 1.f;

    /**
     * View direction allows to override which way the camera looks at.
     *
     * <p>Our engine defaults to +Y forward, since that is the forward transformation direction, but
     * glTF uses a different view direction.
     */
    @Getter private final Vector3f mViewDirection = new Vector3f(0f, 1f, 0f);

    private Matrix4f mToView = new Matrix4f();
    private final Vector3f mTmpPos = new Vector3f();
    private final Vector3f mTmpDir = new Vector3f();

    /**
     * Get the current projection matrix.
     *
     * <p>This method will update the camera's projection matrix and return it back
     *
     * @return the projection matrix for the camera. The same as {@link #mProj}
     */
    public Matrix4fc getProj() {
        switch (mProjection) {
            case PERSPECTIVE:
                mProj.setPerspective(mFov, mAspectRatio, mNearPlane, mFarPlane, true);
                break;
            case ORTHOGRAPHIC:
                mProj.setOrtho(
                        -mOrthographicSize * 0.5f * mAspectRatio,
                        mOrthographicSize * 0.5f * mAspectRatio,
                        -mOrthographicSize * 0.5f,
                        mOrthographicSize * 0.5f,
                        mNearPlane,
                        mFarPlane,
                        true);
                break;
        }

        mProj.m11(-mProj.m11());

        return mProj;
    }

    Matrix4f mInvProj = new Matrix4f();
    Vector4f mNear = new Vector4f(0, 0, -1, 1);
    Vector4f mFar = new Vector4f(0, 0, 1, 1);

    /**
     * Project normalised screen coordinates to world direction vector.
     *
     * <p>See <a
     * href="https://stackoverflow.com/questions/7692988/opengl-math-projecting-screen-space-to-world-space-coords">here</a>
     *
     * @param x x screen coordinate, in [-1; 1] range.
     * @param y y screen coordinate, in [-1; 1] range.
     * @param dest destination vector to project to.
     * @return dest.
     */
    public Vector3f screenToWorldDir(float x, float y, Vector3f dest) {
        mInvProj = getProj().mul(getView(), mInvProj).invert();

        mNear.x = mFar.x = x;
        mNear.y = mFar.y = y;
        mNear.z = -1;
        mNear.w = mFar.z = mFar.w = 1;

        mNear.mul(mInvProj);
        mFar.mul(mInvProj);

        mNear.div(mNear.w);
        mFar.div(mFar.w);

        dest.set(mFar.x - mNear.x, mFar.y - mNear.y, mFar.z - mNear.z);

        return dest.div(dest.length());
    }

    /**
     * Project normalized screen coordinates to a plane defined by a transform.
     *
     * <p>This method will take screen coordinates, and create a local vector within transform,
     * where the camera ray intersects the Z plane defined by it.
     *
     * <p>It accounts for any scaling, transformation, and rotation that the transform may have.
     *
     * @param transform target transform to project to.
     * @param height height above target transform to project to.
     * @param x x screen coordinate in [-1; 1] range.
     * @param y y screen coordinate in [-1; 1] range.
     * @param dest destination vector to project to.
     * @return dest.
     */
    public Vector3f screenToPlane(
            Transform transform, float height, float x, float y, Vector3f dest) {
        screenToWorldDir(x, y, dest);

        getGameObject().getTransform().getWorldMatrix().getTranslation(mTmpPos);
        dest.add(mTmpPos);

        transform.getInvWorldMatrix().transformPosition(dest);
        transform.getInvWorldMatrix().transformPosition(mTmpPos);

        dest.sub(0, 0, height);
        mTmpPos.sub(0, 0, height);

        float heightDiff = mTmpPos.z() - dest.z();
        float moveBy = mTmpPos.z() / heightDiff;

        return dest.sub(mTmpPos).mul(moveBy).add(mTmpPos);
    }

    /**
     * Update the screen's aspect ratio.
     *
     * @param width current screen width.
     * @param height current screen height.
     */
    public void updateAspectRatio(int width, int height) {
        mAspectRatio = (float) width / (float) height;
    }

    /**
     * Get world to view transformation matrix.
     *
     * @return world to view space transformation matrix.
     */
    public Matrix4fc getView() {
        Matrix4fc worldMatrix = getGameObject().getTransform().getWorldMatrix();

        Vector3f pos = worldMatrix.getTranslation(mTmpPos);
        Vector3f dir = worldMatrix.transformDirection(mViewDirection, mTmpDir.zero());

        return mToView.identity().lookAt(pos, dir.add(pos), UP_DIR);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        Scene.getActiveScene().registerSingleton(this);
    }

    @Override
    public void onDestroy() {}
}
