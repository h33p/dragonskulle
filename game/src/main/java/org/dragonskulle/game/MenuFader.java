/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.materials.PBRHighlightMaterial;

/**
 * Fade highlight menu objects!
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class MenuFader extends Component implements IOnAwake, IFrameUpdate {

    @Getter @Setter private float mTotalTime;
    @Getter @Setter private float mPhaseShift;
    @Getter @Setter private float mAlphaMul = 1f;
    @Getter @Setter private float mFadeSpeed = 1f;

    private List<Reference<HighlightControls>> mHighlightControls = new ArrayList<>();

    @Override
    public void onAwake() {
        getGameObject().getComponentsInChildren(HighlightControls.class, mHighlightControls);
    }

    @Override
    public void frameUpdate(float deltaTime) {

        mTotalTime += deltaTime * mFadeSpeed;

        float totalTime = mTotalTime + mPhaseShift * mFadeSpeed;

        float[] rgb =
                Color.getHSBColor(totalTime * 0.1f * mFadeSpeed / (float) Math.PI, 0.9f, 1f)
                        .getRGBComponents(null);

        for (Reference<HighlightControls> ctrlRef : mHighlightControls) {
            HighlightControls ctrl = ctrlRef.get();

            ctrl.setHighlight(
                    rgb[0],
                    rgb[1],
                    rgb[2],
                    Math.max(-0.4f + (float) Math.sin(totalTime + mFadeSpeed / 2f) * 1.4f, 0f)
                            * mAlphaMul);

            for (PBRHighlightMaterial mat : ctrl.getHighlightMaterials()) {
                mat.setMinLerp(0.1f);
                mat.setMaxDist(0.6f);
            }
        }
    }

    @Override
    public void onDestroy() {}
}
