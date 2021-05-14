/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.experimental.Accessors;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.input.Action;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.ICharEvent;
import org.dragonskulle.ui.UIManager.UIBuildableComponent;

/**
 * Class describing a interactive UI input box.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIInputBox extends UIBuildableComponent implements IOnAwake, IFrameUpdate {

    public static final Action CURSOR_LEFT = new Action("CURSOR_LEFT", false);
    public static final Action CURSOR_RIGHT = new Action("CURSOR_RIGHT", false);
    public static final Action CURSOR_START = new Action("CURSOR_START", false);
    public static final Action CURSOR_END = new Action("CURSOR_END", false);
    public static final Action INPUT_BACKSPACE = new Action("INPUT_BACKSPACE", false);
    public static final Action INPUT_DELETE = new Action("INPUT_DELETE", false);
    public static final Action INPUT_FINISH = new Action("INPUT_FINISH", false);

    private static final float REPEAT_TIME = 0.5f;

    private Reference<UIButton> mButton;
    private Reference<UIText> mText;

    private int mPosition = 0;
    private final String mInitialText;

    private final ICharEvent mOnCharInner =
            (c) -> {
                String text = mText.get().getText();
                text = text.substring(0, mPosition) + c + text.substring(mPosition);
                mText.get().setText(text);
                mPosition++;
                mText.get().setCursorPos(mPosition);
            };

    private Reference<ICharEvent> mOnChar;

    /** Default constructor for the input box. */
    public UIInputBox() {
        this("");
    }

    /**
     * Constructor for {@link UIInputBox}.
     *
     * @param text initial text to set.
     */
    public UIInputBox(String text) {
        mInitialText = text;
    }

    /**
     * Get the input of the input box.
     *
     * @return current input in the input box.
     */
    public String getInput() {
        return mText.get().getText();
    }

    /**
     * Set the current text of the input box.
     *
     * @param text new text to set.
     */
    public void setText(String text) {
        mText.get().setText(text);
    }

    /**
     * Stop grabbing input when clicked something else.
     *
     * @return {@code true} if clicked something else, and should stop grabbing input, {@code false}
     *     otherwise.
     */
    private boolean clickedSomethingElse() {
        if (!UIButton.UI_PRESS.isActivated()) {
            return false;
        }

        Reference<UIRenderable> hovered = UIManager.getInstance().getHoveredObject();

        return hovered != mButton.get().getRenderable();
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (!Reference.isValid(mText)) {
            if (Reference.isValid(mButton)) {
                mText = mButton.get().getLabelText();
            }

            if (!Reference.isValid(mText)) {
                return;
            }
        }

        if (Actions.getOnChar() != mOnCharInner) {
            mText.get().setCursorPos(-1);
            return;
        }

        if (INPUT_FINISH.isActivated() || clickedSomethingElse()) {
            Actions.setOnChar(null);
            mPosition = -1;
        }

        String text = mText.get().getText();

        if (CURSOR_END.isJustActivated()) {
            mPosition = text.length();
        }

        if (CURSOR_LEFT.isJustActivated() || CURSOR_LEFT.getTimeActivated() > REPEAT_TIME) {
            mPosition--;
            if (mPosition < 0) {
                mPosition = 0;
            }
        }

        if (CURSOR_RIGHT.isJustActivated() || CURSOR_RIGHT.getTimeActivated() > REPEAT_TIME) {
            mPosition++;
            if (mPosition >= text.length()) {
                mPosition = text.length();
            }
        }

        if (CURSOR_END.isJustActivated()) {
            mPosition = text.length();
        }

        if (CURSOR_START.isJustActivated()) {
            mPosition = 0;
        }

        if (INPUT_DELETE.isJustActivated() || INPUT_DELETE.getTimeActivated() > REPEAT_TIME) {
            if (mPosition < text.length()) {
                text = text.substring(0, mPosition) + text.substring(mPosition + 1);
                mText.get().setText(text);
            }
        }

        if (INPUT_BACKSPACE.isJustActivated() || INPUT_BACKSPACE.getTimeActivated() > REPEAT_TIME) {
            if (mPosition > 0) {
                text = text.substring(0, mPosition - 1) + text.substring(mPosition);
                mText.get().setText(text);
                mPosition--;
            }
        }

        mText.get().setCursorPos(mPosition);
    }

    @Override
    public void onAwake() {
        mOnChar = new Reference<>(mOnCharInner);

        UIButton button =
                new UIButton(
                        mInitialText,
                        (__, ___) -> {
                            if (Reference.isValid(mText)) {
                                mPosition = mText.get().getText().length();
                                Actions.setOnChar(mOnChar);
                                mText.get().setCursorPos(mPosition);
                            }
                        });

        getGameObject().addComponent(button);

        mButton = button.getReference(UIButton.class);
    }

    @Override
    protected void onDestroy() {
        mOnChar.clear();
        if (Reference.isValid(mButton)) {
            mButton.get().destroy();
        }
    }
}
