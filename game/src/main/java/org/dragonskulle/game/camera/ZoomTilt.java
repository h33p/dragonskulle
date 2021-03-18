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
import org.dragonskulle.utils.MathUtils;

/**
 * Allows to tilt the camera based on current zoom level
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class ZoomTilt extends Component implements IFrameUpdate, IOnAwake, IZoomNotify {
    @Getter @Setter public float mMinTilt = -30f;
    @Getter @Setter public float mMaxTilt = -75f;

    @Getter @Setter private float mZoomLevel = 0.f;

    private transient Transform3D mTransform;

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mTransform != null) {
            float angle = MathUtils.lerp(mMinTilt, mMaxTilt, mZoomLevel);
            mTransform.setRotationDeg(angle, 0, 0);
        }
    }

    @Override
    protected void onDestroy() {}
}
