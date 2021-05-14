/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.ui.UIManager.UIBuildableComponent;

/**
 * Class describing a drop down menu.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIDropDown extends UIBuildableComponent implements IOnAwake, IFrameUpdate {

    /** Ran on dropdown open. */
    @Setter IDropDownEvent mOnOpen;

    /** Ran on dropdown hide. */
    @Setter IDropDownEvent mOnHide;

    /** Interface that is invoked when an event occurs for the drop down. */
    public static interface IDropDownEvent {
        /**
         * Handle the drop down event.
         *
         * @param dropDown calling drop down.
         */
        void handle(UIDropDown dropDown);
    }

    /** Available options in the drop down. */
    private String[] mOptions;

    /**
     * Currently selected UI element. If outside the range of {@link mOptions}, nothing will be
     * displayed as selected. Thus, value of {@code -1} will never have a selected element
     */
    @Getter @Setter private int mSelected = -1;

    /**
     * Event which gets called whenever an element gets selected by the user.
     *
     * <p>This event will only be called on user clicks, it will not be invoked by setting {@link
     * mSelected}. Once the event is invoked, {@link mSelected} will already have a new value set.
     */
    @Getter @Setter private IDropDownEvent mOnSelect;

    /**
     * Currently displayed item. By default set to {@code -2} to trigger a menu update in {@link
     * frameUpdate}
     */
    private int mDisplayed = -2;

    private Reference<UIButton> mButton;
    private final ArrayList<GameObject> mOptionObjects = new ArrayList<>();

    /** Default constructor for {@link UIDropDown}. */
    public UIDropDown() {}

    /**
     * {@link UIDropDown} constructor.
     *
     * @param options list of options to be selectable
     */
    public UIDropDown(String... options) {
        setOptions(options);
    }

    /**
     * {@link UIDropDown} constructor.
     *
     * @param selected which option will initially be selected
     * @param options list of options to be selectable
     */
    public UIDropDown(int selected, String... options) {
        this(options);
        mSelected = selected;
    }

    /**
     * {@link UIDropDown} constructor.
     *
     * @param selected which option will initially be selected
     * @param onSelect event to be invoked when selection changes
     * @param options list of options to be selectable
     */
    public UIDropDown(int selected, IDropDownEvent onSelect, String... options) {
        this(selected, options);
        mOnSelect = onSelect;
    }

    /**
     * Get the string value of the selected option.
     *
     * @return String value of the selected option, or {@code null} if no valid option is selected
     */
    public String getSelectedOption() {
        return hasSelection() ? mOptions[mSelected] : null;
    }

    /**
     * Check whether the dropdown has a selection.
     *
     * @return {@code true} if the dropdown has a selection, {@code false} if it doesn't
     */
    public boolean hasSelection() {
        return mOptions != null && mSelected >= 0 && mSelected < mOptions.length;
    }

    /**
     * Set options to be selectable.
     *
     * <p>This method will also reset selected option, and close the drop-down
     *
     * @param options new options to be selectable
     */
    public void setOptions(String... options) {
        mOptions = options;
        mSelected = -1;
        mDisplayed = -2;
        cleanOptions();
    }

    /** Display options if haven't already. */
    private void showOptions() {
        if (mOptionObjects.size() != 0 || mOptions == null) {
            return;
        }
        for (int i = 0; i < mOptions.length; i++) {
            final int ii = i;
            GameObject option =
                    new GameObject(
                            "option_" + i,
                            new TransformUI(true),
                            (handle) -> {
                                handle.setDepthOffset(5);
                                UIButton button =
                                        new UIButton(mOptions[ii], (__, ___) -> select(ii));
                                handle.addComponent(button);
                                TransformUI transform = handle.getTransform(TransformUI.class);
                                transform.setParentAnchor(0, ii + 1, 1, ii + 2);
                            });
            mOptionObjects.add(option);
        }
        getGameObject().addChildren(mOptionObjects);
        if (mOnOpen != null) {
            mOnOpen.handle(this);
        }
    }

    /** Stop showing options, if they are still being shown. */
    private void cleanOptions() {
        if (mOptionObjects.size() == 0) {
            return;
        }

        for (GameObject go : mOptionObjects) {
            go.destroy();
        }

        mOptionObjects.clear();
        if (mOnHide != null) {
            mOnHide.handle(this);
        }
    }

    /** Toggle between showing and not showing the option list. */
    private void toggleOptions() {

        if (Reference.isValid(mButton)) {
            mButton.get().setLockPressed(mOptionObjects.size() == 0);
        }

        if (mOptionObjects.size() == 0) {
            showOptions();
        } else {
            cleanOptions();
        }
    }

    /**
     * Select an element and invoke OnSelect event.
     *
     * @param index the index of the selected item
     */
    private void select(int index) {
        setSelected(index);

        if (mOnSelect != null) {
            mOnSelect.handle(this);
        }
    }

    @Override
    public void onAwake() {
        UIButton button = new UIButton((__, ___) -> toggleOptions());
        button.setIcon(UIManager.getInstance().getAppearance().getDropDownIconTexture().clone());
        getGameObject().addComponent(button);
        mButton = button.getReference(UIButton.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {

        if (!Reference.isValid(mButton)) {
            return;
        }

        UIButton button = mButton.get();

        if (UIButton.UI_PRESS.isJustDeactivated()
                && UIManager.getInstance().getHoveredObject() != button.getRenderable()) {
            button.setLockPressed(false);
            cleanOptions();
        }

        if (mDisplayed == mSelected) {
            return;
        }

        if (!Reference.isValid(button.getLabelText())) {
            return;
        }

        UIText text = button.getLabelText().get();

        mDisplayed = mSelected;

        if (mDisplayed < 0 || mDisplayed >= mOptions.length) {
            text.setText("");
            return;
        }

        text.setText(mOptions[mDisplayed]);
    }

    @Override
    protected void onDestroy() {}
}
