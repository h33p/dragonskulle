/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

/**
 * A counter which is displayed in the menu for how many tokens the player currently has.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UITokenCounter extends Component implements IOnAwake {
    private Reference<UITextRect> mTextRect;
    private Reference<UIText> mTokens;

    /**
     * Sets the text in the counter to "Tokens: " + newTokens.
     *
     * @param newTokens the new tokens
     */
    public void setLabelReference(int newTokens) {
        if (Reference.isValid(mTokens)) {
            mTokens.get().setText("Tokens: " + newTokens);
        } else {
            if (Reference.isValid(mTextRect)) {
                mTokens = mTextRect.get().getLabelText();
                if (Reference.isValid(mTokens)) {
                    mTextRect.get().getLabelText().get().setText("Tokens: " + newTokens);
                }
            }
        }
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        final TransformUI transform = getGameObject().getTransform(TransformUI.class);
        transform.setMaintainAspect(false);
        transform.setParentAnchor(0.37f, 0.08f, 0.37f, 0.08f);
        transform.setMargin(-0.285f, -0.034f, 0.285f, 0.034f);

        UITextRect textRect = new UITextRect("Tokens: 0");

        getGameObject().addComponent(textRect);
        textRect.setRectTexture(GameUIAppearance.getInfoBoxTexture());

        mTextRect = textRect.getReference(UITextRect.class);
    }
}
