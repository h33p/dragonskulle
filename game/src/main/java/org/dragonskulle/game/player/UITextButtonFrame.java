package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.ui.UIButton;

/**
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UITextButtonFrame {
    @Getter
    private final String mText;
    @Getter
    private final UIButton.IButtonEvent mOnClick;

    UITextButtonFrame(String mText, UIButton.IButtonEvent mOnClick) {
        this.mText = mText;
        this.mOnClick = mOnClick;
    }
}
