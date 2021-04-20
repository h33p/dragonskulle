/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.ui.UIManager.UIBuildableComponent;

/**
 * Essentially a {@code UIRenderable} that extends UIBuildableComponent so it can be used in builders.
 */
public class UIFlatImage extends UIBuildableComponent implements IOnAwake, IUIBuildHandler {
    final SampledTexture mTexture;

    /**
     * Constructor.
     *
     * @param texture the texture
     */
    public UIFlatImage(SampledTexture texture) {
        mTexture = texture;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        getGameObject().addComponent(new UIRenderable(mTexture));
    }
}
