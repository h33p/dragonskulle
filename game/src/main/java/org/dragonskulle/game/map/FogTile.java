/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.IColouredMaterial;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.dragonskulle.utils.MathUtils;

/**
 * @author Aurimas Blažulionis
 *     <p>This component draws a visual fog of war for the players
 */
@Accessors(prefix = "m")
@Log
public class FogTile extends Component implements IOnAwake, IFrameUpdate {

    @Getter @Setter private float mFadeTime = 0.2f;

    private float mTarget = 1f;

    private Reference<Renderable> mRenderable;
    private float mFadeValue = 1f;

    public void setFog(boolean enabled) {
        mTarget = enabled ? 1f : -1f;
    }

    @Override
    public void onAwake() {
        mRenderable = getGameObject().getComponent(Renderable.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mRenderable == null || !mRenderable.isValid()) {
            return;
        }

        mFadeValue = MathUtils.clamp(mFadeValue + mTarget * deltaTime / mFadeTime, 0f, 1);

        IColouredMaterial colMat = mRenderable.get().getMaterial(IColouredMaterial.class);

        if (colMat != null) {
            colMat.setAlpha(mFadeValue);
        }

        if (colMat instanceof PBRMaterial) {
            PBRMaterial pbrMat = (PBRMaterial) colMat;
            pbrMat.setAlphaBlend(mFadeValue < 1f && mFadeValue > 0f);
            pbrMat.setAlphaCutoff(0.01f);
        }
    }

    @Override
    protected void onDestroy() {}
}
