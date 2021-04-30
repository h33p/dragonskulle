/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
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
public class UITokenCounter extends Component implements IOnAwake, IFixedUpdate {
    private Reference<UITextRect> mTextRect;
    private Reference<UIText> mTokens;
    @Setter private int mTargetTokens = 0;

    @Setter private float mCurTokens = 0;

    /**
     * Sets the text in the counter to "Tokens: " + newTokens.
     *
     * @param newTokens the new tokens
     */
    public void setLabelReference(int newTokens) {
        if (newTokens < mTargetTokens) {
            // if negative force
            setTokens(newTokens);
        }
        mTargetTokens = newTokens;
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

        UITextRect textRect = new UITextRect(String.format("Tokens: %5d", 0));

        getGameObject().addComponent(textRect);
        textRect.setRectTexture(GameUIAppearance.getInfoBoxTexture());

        mTextRect = textRect.getReference(UITextRect.class);
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (mCurTokens == mTargetTokens) return;
        float step = Math.abs(mTargetTokens - mCurTokens) / 2;
        float incrToken = mCurTokens + (mCurTokens < mTargetTokens ? step : -step);
        setTokens(incrToken);
    }

    /**
     * Sets tokens to aim incrementer for.
     *
     * @param incrToken the incr token
     */
    private void setTokens(float incrToken) {
        setCurTokens(incrToken);
        setVisibleTokens(incrToken);
    }

    /**
     * Sets visible tokens.
     *
     * @param incrToken the incr token
     */
    private void setVisibleTokens(float incrToken) {
        if (Reference.isValid(mTokens)) {
            mTokens.get().setText(String.format("Tokens: %5d", Math.round(incrToken)));
        } else {
            if (Reference.isValid(mTextRect)) {
                mTokens = mTextRect.get().getLabelText();
                if (Reference.isValid(mTokens)) {
                    mTextRect
                            .get()
                            .getLabelText()
                            .get()
                            .setText(String.format("Tokens: %5d", Math.round(incrToken)));
                }
            }
        }
    }
}
