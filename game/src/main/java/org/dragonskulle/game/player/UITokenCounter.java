/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UITextRect;

/** @author Oscar L */
@Accessors(prefix = "m")
public class UITokenCounter extends Component implements IOnAwake {
    private Reference<UITextRect> mTextRect;

    public void setLabelReference(int newTokens) {
        if (Reference.isValid(mTextRect) && Reference.isValid(mTextRect.get().getLabelText())) {
            mTextRect.get().getLabelText().get().setText("Tokens: " + newTokens);
        }
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
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
