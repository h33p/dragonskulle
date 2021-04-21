/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.utils.MathUtils;

/**
 * This class allows to control height of hexagon tiles, smoothly transitioning between different
 * heights
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class HeightController extends Component implements IOnAwake, IFrameUpdate {

    private TransformHex mHexTransform;

    @Getter @Setter private float mTargetHeight = 0f;

    @Getter @Setter private float mSpeed = 3f;

    @Override
    public void onAwake() {
        mHexTransform = getGameObject().getTransform(TransformHex.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mHexTransform == null) {
            return;
        }

        float curHeight = mHexTransform.getHeight();

        float newHeight =
                MathUtils.lerp(curHeight, mTargetHeight, Math.min(deltaTime * mSpeed, 1f));

        mHexTransform.setHeight(newHeight);
    }

    @Override
    protected void onDestroy() {}
}
