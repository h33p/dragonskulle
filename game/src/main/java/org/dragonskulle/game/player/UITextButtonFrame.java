/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.ui.UIButton;

/**
 * A class to describe how a button should be built without building it.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UITextButtonFrame {
    @Getter private final String mText;
    @Getter private final UIButton.IButtonEvent mOnClick;
    @Getter private final String mId;
    @Getter private final boolean mStartEnabled;

    /**
     * Instantiates a new button frame.
     *
     * @param id the id
     * @param mText the text
     * @param mOnClick the onClick handler
     * @param startEnabled start enabled or not
     */
    UITextButtonFrame(
            String id, String mText, UIButton.IButtonEvent mOnClick, boolean startEnabled) {
        this.mId = id;
        this.mText = mText;
        this.mOnClick = mOnClick;
        this.mStartEnabled = startEnabled;
    }
}
