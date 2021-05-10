/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.game.camera.ScrollTranslate.IZoomNotify;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.utils.MathUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Allows to control object with keyboard.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class KeyboardMovement extends Component implements IFrameUpdate, IOnAwake, IZoomNotify {
    @Getter @Setter public float mMinMoveSpeed = 5f;
    @Getter @Setter public float mMaxMoveSpeed = 90f;

    @Getter @Setter public float mRotateSpeed = 180f;

    @Getter @Setter private float mZoomLevel = 0.f;

    private transient Transform3D mTransform;

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mTransform != null) {

            float moveSpeed = MathUtils.lerp(mMinMoveSpeed, mMaxMoveSpeed, mZoomLevel);

            float xAxis = GameActions.RIGHT.isActivated() ? 1 : 0;
            xAxis -= GameActions.LEFT.isActivated() ? 1 : 0;
            xAxis *= moveSpeed * deltaTime;

            float yAxis = GameActions.UP.isActivated() ? 1 : 0;
            yAxis -= GameActions.DOWN.isActivated() ? 1 : 0;
            yAxis *= moveSpeed * deltaTime;

            float rotAxis = GameActions.ROTATE_RIGHT.isActivated() ? 1 : 0;
            rotAxis -= GameActions.ROTATE_LEFT.isActivated() ? 1 : 0;
            rotAxis *= mRotateSpeed * deltaTime;

            // Extracted from Transform as to not modify the normal transform code
            Vector3f pos = mTransform.getPosition();
            Quaternionf rot = mTransform.getRotation();
            Vector3f mTmpForward = new Vector3f();
            mTmpForward.set(xAxis, yAxis, 0f).rotate(rot);
            pos.add(mTmpForward);
            MathUtils.clampVector(pos, 42f);
            mTransform.setPosition(pos);
            rot.rotateXYZ(0f, 0f, MathUtils.DEG_TO_RAD * -rotAxis);
            mTransform.setRotation(rot);
        }
    }

    @Override
    protected void onDestroy() {}
}
