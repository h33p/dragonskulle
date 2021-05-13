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
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.materials.PBRHighlightMaterial;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.IColouredMaterial;

/**
 * Fade highlight menu objects.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class MenuFader extends Component implements IOnAwake, IFrameUpdate {

    @Setter private float mTotalTime;
    @Getter @Setter private float mPhaseShift;
    @Getter @Setter private float mAlphaMul = 1f;
    @Getter @Setter private float mFadeSpeed = 1f;
    @Getter @Setter private Reference<Renderable> mTitleRenderable;
    @Getter @Setter private String mTitleString;

    private List<Reference<HighlightControls>> mHighlightControls = new ArrayList<>();

    @Override
    public void onAwake() {
        getGameObject().getComponentsInChildren(HighlightControls.class, mHighlightControls);

        if (mTitleString != null) {
            GameObject mObject = Scene.getActiveScene().findRootObject(mTitleString);

            if (mObject != null) {
                mTitleRenderable = mObject.getComponent(Renderable.class);
            }
        }
    }

    /**
     * Get the total time with phase shift applied.
     *
     * @return total time with phase shift offset.
     */
    public float getTotalTime() {
        return mTotalTime + mPhaseShift * mFadeSpeed;
    }

    /**
     * Get the total time with sine applied to it.
     *
     * @param off offset of total time.
     * @return the sine time.
     */
    public float sineTime(float off) {
        return (float) Math.sin(off + getTotalTime() + mFadeSpeed / 2f);
    }

    @Override
    public void frameUpdate(float deltaTime) {

        mTotalTime += deltaTime * mFadeSpeed;

        float totalTime = getTotalTime();

        float[] rgb =
                Color.getHSBColor(totalTime * 0.1f * mFadeSpeed / (float) Math.PI, 0.9f, 1f)
                        .getRGBComponents(null);

        float sineTime = sineTime(0);
        float col = -0.4f + sineTime * 1.4f;

        for (Reference<HighlightControls> ctrlRef : mHighlightControls) {
            HighlightControls ctrl = ctrlRef.get();

            ctrl.setHighlight(rgb[0], rgb[1], rgb[2], Math.max(col, 0f) * mAlphaMul);

            for (PBRHighlightMaterial mat : ctrl.getHighlightMaterials()) {
                mat.setMinLerp(0.1f);
                mat.setMaxDist(0.6f);
            }
        }

        if (Reference.isValid(mTitleRenderable)) {
            IColouredMaterial colouredMat =
                    mTitleRenderable.get().getMaterial(IColouredMaterial.class);

            if (colouredMat != null) {
                float time = sineTime * 0.5f + 0.5f;
                colouredMat.getColour().set(rgb[0], rgb[1], rgb[2], 1);
                colouredMat.getColour().mul(time * 0.5f + 0.5f);
            }
        }
    }

    @Override
    public void onDestroy() {}
}
