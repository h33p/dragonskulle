/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/**
 * A counter which is displayed in the menu for how many tokens the player currently has.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UITokenCounter extends Component implements IOnStart {
    /**
     * Sets the text in the counter to "Tokens: " + newTokens.
     *
     * @param newTokens the new tokens
     */
    public void setLabelReference(int newTokens) {
        Reference<UIButton> buttonRef = getGameObject().getComponent(UIButton.class);
        if (buttonRef != null && buttonRef.isValid()) {
            Reference<UIText> txt = buttonRef.get().getLabelText();
            if (txt != null && txt.isValid()) {
                txt.get().setText("Tokens: " + newTokens);
            }
        }
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        final TransformUI transform = getGameObject().getTransform(TransformUI.class);
        transform.setMaintainAspect(false);
        transform.setParentAnchor(0.37f, 0.08f, 0.37f, 0.08f);
        transform.setMargin(-0.285f, -0.034f, 0.285f, 0.034f);
        getGameObject().addComponent(new UIRenderable(new SampledTexture("ui/info_box.png")));

        getGameObject()
                .addComponent(
                        new UIButton(
                                new UIText(
                                        new Vector3f(0f, 0f, 0f),
                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                        "Tokens: 0")));
    }
}
