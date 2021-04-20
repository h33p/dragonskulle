/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.ui.UIManager.UIBuildableComponent;

public class UIFlatImage extends UIBuildableComponent implements IOnAwake, IUIBuildHandler {
    private final SampledTexture mTexture;
    private final boolean mHoverable;

    public UIFlatImage(SampledTexture texture, boolean hoverable) {
        mTexture = texture;
        mHoverable = hoverable;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        UIRenderable rend = new UIRenderable(mTexture);
        rend.setHoverable(mHoverable);
        getGameObject().addComponent(rend);
    }
}
