/* (C) 2021 DragonSkulle */
package org.dragonskulle.devtools;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.Renderer;
import org.dragonskulle.ui.UIText;

/**
 * Render debug text display
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class RenderDebug extends Component implements IOnAwake, IFrameUpdate {
    private Reference<UIText> mText;

    @Override
    public void onAwake() {
        mText = getGameObject().getComponent(UIText.class);

        if (mText == null) {
            UIText text = new UIText();
            getGameObject().addComponent(text);
            mText = text.getReference(UIText.class);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mText != null && mText.isValid()) {
            Renderer rend = Engine.getInstance().getGLFWState().getRenderer();

            int instancedDrawCalls = rend.getInstancedCalls();
            int slowDrawCalls = rend.getSlowCalls();

            mText.get()
                    .setText(
                            String.format(
                                    "FPS: %.2f (%.1f ms)\nInstanced Draws: %d\nSlow Draws: %d\nIB Size: %d\nMB Size: VB - %d | IB - %d",
                                    1.0 / (double) deltaTime,
                                    deltaTime * 1000.f,
                                    instancedDrawCalls,
                                    slowDrawCalls,
                                    rend.getInstancedCalls(),
                                    rend.getVertexBufferSize(),
                                    rend.getIndexBufferSize()));
        }
    }

    @Override
    public void onDestroy() {}
}
