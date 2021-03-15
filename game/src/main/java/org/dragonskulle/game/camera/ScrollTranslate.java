/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.utils.MathUtils;
import org.joml.Math;
import org.joml.Vector3f;

/**
 * Allows to control object translation with scroll wheel
 *
 * @author Aurimas Blažulionis
 */
public class ScrollTranslate extends Component implements IFrameUpdate, IOnAwake {
    public float minSpeed = 1f;
    public float maxSpeed = 15f;
    public float powFactor = 2f;
    public Vector3f startPos = new Vector3f();
    public Vector3f endPos = new Vector3f();

    private Vector3f mTmpTransform = new Vector3f();
    private float mCurLerp = 0f;
    private transient Transform3D mTransform;
    private KeyboardMovement mMovement;

    public ScrollTranslate(KeyboardMovement movement) {
        mMovement = movement;
    }

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mTransform != null) {
            float direction = GameActions.ZOOM_IN.isActivated() ? 1 : 0;
            direction -= GameActions.ZOOM_OUT.isActivated() ? 1 : 0;
            mCurLerp +=
                    direction
                            * MathUtils.lerp(
                                    minSpeed,
                                    maxSpeed,
                                    (float) java.lang.Math.pow(mCurLerp, powFactor))
                            * deltaTime;
            mCurLerp = Math.min(1.f, Math.max(mCurLerp, 0.f));
            startPos.lerp(endPos, mCurLerp, mTmpTransform);
            mTransform.setPosition(mTmpTransform);
            if (mMovement != null) mMovement.setZoomLevel(mCurLerp);
        }
    }

    @Override
    protected void onDestroy() {}
}
