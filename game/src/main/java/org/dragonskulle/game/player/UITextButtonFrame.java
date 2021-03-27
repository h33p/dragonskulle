/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.ui.UIButton;

/** @author Oscar L */
@Accessors(prefix = "m")
public class UITextButtonFrame {
    @Getter private final String mText;
    @Getter private final UIButton.IButtonEvent mOnClick;
    @Getter private final String mId;
    @Getter private final boolean mStartEnabled;

    UITextButtonFrame(
            String id, String mText, UIButton.IButtonEvent mOnClick, boolean startEnabled) {
        this.mId = id;
        this.mText = mText;
        this.mOnClick = mOnClick;
        this.mStartEnabled = startEnabled;
    }
}
