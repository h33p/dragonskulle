/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIManager.UIBuildableComponent;

/**
 * Essentially a {@code UIRenderable} that extends UIBuildableComponent so it can be used in
 * builders.
 */
public class UIFlatImage extends UIBuildableComponent implements IOnAwake {
    private final SampledTexture mTexture;
    private final boolean mHoverable;

    /**
     * Constructor.
     *
     * @param texture the texture.
     * @param hoverable whether this image is hoverable or not.
     */
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
