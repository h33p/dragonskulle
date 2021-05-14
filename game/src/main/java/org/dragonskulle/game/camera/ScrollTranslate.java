/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import java.util.Arrays;
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
 * Allows to control object translation with scroll wheel.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class ScrollTranslate extends Component implements IFrameUpdate, IOnAwake {

    /** Ran on zoom change. */
    public static interface IZoomNotify {
        /**
         * Handle zoom level event.
         *
         * @param level new zoom level to set.
         */
        void setZoomLevel(float level);
    }

    /** Controls the minimum zoom speed. */
    @Getter @Setter private float mMinSpeed = 0.01f;

    /** Controls the maximum zoom speed. */
    @Getter @Setter private float mMaxSpeed = 0.1f;

    /**
     * Controls the curvature of scroll speed.
     *
     * <p>Essentially, with values over 1, the less we are zoomed in, the quicker we can zoom in.
     * The more we are zoomed in, the slower additional zoom is.
     */
    @Getter @Setter private float mPowFactor = 1.5f;

    @Getter private final Vector3f mStartPos = new Vector3f();
    @Getter private final Vector3f mEndPos = new Vector3f();

    private Vector3f mTmpTransform = new Vector3f();
    @Getter @Setter private float mTargetLerpTime = 0f;
    private float mZoomLerpTime = 0.1f;
    @Getter private float mZoomLevel = 0f;
    private transient Transform3D mTransform;
    private IZoomNotify[] mNotify;

    /** Constructor for {@link ScrollTranslate}. */
    public ScrollTranslate() {}

    /**
     * Constructor for {@link ScrollTranslate}.
     *
     * @param notify list of components that need to be notified of the zoom level changes.
     */
    public ScrollTranslate(IZoomNotify... notify) {
        mNotify = notify;
    }

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
        mTargetLerpTime = mZoomLevel;
    }

    @Override
    public void frameUpdate(float deltaTime) {

        if (mTransform != null) {
            float direction = (float) -GameActions.getScroll().getAmount();
            mTargetLerpTime +=
                    direction
                            * MathUtils.lerp(
                                    mMinSpeed,
                                    mMaxSpeed,
                                    (float) java.lang.Math.pow(mTargetLerpTime, mPowFactor));

            mTargetLerpTime = Math.min(1.f, Math.max(mTargetLerpTime, 0.f));
            float lerpTime =
                    MathUtils.lerp(
                            mZoomLevel, mTargetLerpTime, Math.min(deltaTime / mZoomLerpTime, 1));

            mZoomLevel = Math.min(1.f, Math.max(lerpTime, 0.f));
            mStartPos.lerp(mEndPos, mZoomLevel, mTmpTransform);
            mTransform.setPosition(mTmpTransform);
            if (mNotify != null) {
                Arrays.stream(mNotify).forEach(n -> n.setZoomLevel(mZoomLevel));
            }
        }
    }

    @Override
    protected void onDestroy() {}
}
