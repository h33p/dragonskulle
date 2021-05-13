/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.IColouredMaterial;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.dragonskulle.utils.MathUtils;

/**
 * Fade tiles in and out.
 *
 * @author Aurimas Blažulionis
 *     <p>This component smoothly fades tiles in and out.
 */
@Accessors(prefix = "m")
public class FadeTile extends Component implements IOnAwake, IFrameUpdate {

    @Getter @Setter private String mChildName = "";
    @Getter @Setter private float mFadeTime = 0.4f;
    @Getter @Setter private boolean mDestroyOnFadeOut = false;

    private float mAlphaMul = 1f;

    private float mTarget = 1f;

    private Reference<Renderable> mRenderable;
    @Getter @Setter private float mFadeValue = 1f;

    @Getter private float mTargetHeight = 0f;

    private Reference<HeightController> mHeightController;

    /**
     * Set the state of the tile.
     *
     * @param enabled whether the tile should show or not.
     * @param height target height of the tile.
     */
    public void setState(boolean enabled, float height) {
        mTarget = enabled ? 1f : -1f;
        mTargetHeight = height;
    }

    @Override
    public void onAwake() {
        if (!Reference.isValid(mRenderable)) {
            GameObject rendObj = getGameObject();

            if (!mChildName.equals("")) {
                rendObj = rendObj.findChildByName(mChildName);
            }

            mRenderable = rendObj.getComponent(Renderable.class);
        }

        mHeightController = getGameObject().getComponent(HeightController.class);
        IColouredMaterial colMat = mRenderable.get().getMaterial(IColouredMaterial.class);

        if (colMat != null) {
            mAlphaMul = colMat.getAlpha();
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {

        if (Reference.isValid(mHeightController)) {
            mHeightController.get().setTargetHeight(mTargetHeight);
        }

        if (!Reference.isValid(mRenderable)) {
            return;
        }

        mFadeValue = MathUtils.clamp(mFadeValue + mTarget * deltaTime / mFadeTime, 0f, 1);

        float alphaValue = mFadeValue * mAlphaMul;

        IColouredMaterial colMat = mRenderable.get().getMaterial(IColouredMaterial.class);

        if (colMat != null) {
            colMat.setAlpha(alphaValue);
        }

        if (colMat instanceof PBRMaterial) {
            PBRMaterial pbrMat = (PBRMaterial) colMat;
            pbrMat.setAlphaBlend(alphaValue < 1f && alphaValue > 0f);
            pbrMat.setAlphaCutoff(0.01f);
        }

        if (mDestroyOnFadeOut && mFadeValue <= 0f) {
            getGameObject().destroy();
        }
    }

    @Override
    protected void onDestroy() {}
}
