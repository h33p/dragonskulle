/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
@Accessors(prefix = "m")
public class ScrollTranslate extends Component implements IFrameUpdate, IOnAwake {
    @Getter @Setter
    /** Controls the minimum zoom speed */
    private float mMinSpeed = 1f;

    @Getter @Setter
    /** Controls the maximum zoom speed */
    private float mMaxSpeed = 15f;

    @Getter @Setter
    /**
     * Controls the curvature of scroll speed.
     *
     * <p>Essentially, with values over 1, the less we are zoomed in, the quicker we can zoom in.
     * The more we are zoomed in, the slower additional zoom is.
     */
    private float mPowFactor = 2f;

    @Getter private final Vector3f mStartPos = new Vector3f();
    @Getter private final Vector3f mEndPos = new Vector3f();

    private Vector3f mTmpTransform = new Vector3f();
    private float mCurLerp = 0f;
    private transient Transform3D mTransform;
    private KeyboardMovement mMovement;

    /** Constructor for {@link ScrollTranslate} */
    public ScrollTranslate() {}

    /**
     * Constructor for {@link ScrollTranslate}
     *
     * @param movement if non-null, this reference will be told every frame how much we are zoomed
     *     in.
     */
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
                                    mMinSpeed,
                                    mMaxSpeed,
                                    (float) java.lang.Math.pow(mCurLerp, mPowFactor))
                            * deltaTime;
            mCurLerp = Math.min(1.f, Math.max(mCurLerp, 0.f));
            mStartPos.lerp(mEndPos, mCurLerp, mTmpTransform);
            mTransform.setPosition(mTmpTransform);
            if (mMovement != null) mMovement.setZoomLevel(mCurLerp);
        }
    }

    @Override
    protected void onDestroy() {}
}
