/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.input.Action;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Class describing a interactive UI button.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIButton extends UITextRect implements IFrameUpdate {

    /** Simple interface describing button callback events. */
    public interface IButtonEvent {
        /**
         * Method for handling the event.
         *
         * @param button calling button
         * @param deltaTime forwarded deltaTime from frameUpdate
         */
        public void eventHandler(UIButton button, float deltaTime);
    }

    /** Input action that needs to be bound for UI button presses to function. */
    public static final Action UI_PRESS = new Action("UI_PRESS", false);

    private Vector4fc mRegularColour = new Vector4f(1f);
    private Vector4fc mHoveredColour = new Vector4f(0.8f, 0.8f, 0.8f, 1f);
    private Vector4fc mPressedColour = new Vector4f(0.6f, 0.6f, 0.6f, 1f);
    private Vector4fc mDisabledColour = new Vector4f(0.882f, 0.027f, 0.019f, 1f);

    @Getter @Setter private boolean mLockPressed = false;

    private Vector4f mTmpLerp = new Vector4f(1f);

    private float mTransitionTime = 0.1f;

    private boolean mIsEnabled = true;

    private float mCurTimer = 0f;

    private IButtonEvent mOnPressDown;
    private IButtonEvent mOnRelease;
    private IButtonEvent mOnClick;
    private IButtonEvent mOnHover;
    private IButtonEvent mOffHover;
    private IButtonEvent mWhileHover;

    private boolean mLastMouseDown = false;
    private boolean mLastHovered = false;
    private boolean mHadReleasedHover = true;
    private boolean mPressedDown = false;

    /** Default Constructor for UIButton. */
    public UIButton() {
        super();
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     */
    public UIButton(UIText label) {
        super(label);
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     */
    public UIButton(String label) {
        super(label);
    }

    /**
     * Constructor for UIButton.
     *
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(boolean startEnabled) {
        this();
        mIsEnabled = startEnabled;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(String label, boolean startEnabled) {
        this(label);
        mIsEnabled = startEnabled;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     */
    public UIButton(String label, IButtonEvent onClick) {
        this(label);
        mOnClick = onClick;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(String label, IButtonEvent onClick, boolean startEnabled) {
        this(label);
        mIsEnabled = startEnabled;
        mOnClick = onClick;
    }

    /**
     * Constructor for UIButton.
     *
     * @param onClick callback to be called when the button is clicked
     */
    public UIButton(IButtonEvent onClick) {
        this();
        mOnClick = onClick;
    }

    /**
     * Constructor for UIButton.
     *
     * @param onClick callback to be called when the button is clicked
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(IButtonEvent onClick, boolean startEnabled) {
        this();
        mIsEnabled = startEnabled;
        mOnClick = onClick;
    }

    /**
     * Constructor for UIButton.
     *
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     */
    public UIButton(IButtonEvent onClick, IButtonEvent onPressDown, IButtonEvent onRelease) {
        this(onClick);
        mOnPressDown = onPressDown;
        mOnRelease = onRelease;
    }

    /**
     * Constructor for UIButton.
     *
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(
            IButtonEvent onClick,
            IButtonEvent onPressDown,
            IButtonEvent onRelease,
            boolean startEnabled) {
        this(onClick);
        mIsEnabled = startEnabled;
        mOnPressDown = onPressDown;
        mOnRelease = onRelease;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     */
    public UIButton(
            String label, IButtonEvent onClick, IButtonEvent onPressDown, IButtonEvent onRelease) {
        this(label, onClick);
        mOnPressDown = onPressDown;
        mOnRelease = onRelease;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(
            String label,
            IButtonEvent onClick,
            IButtonEvent onPressDown,
            IButtonEvent onRelease,
            boolean startEnabled) {
        this(label, onClick);
        mIsEnabled = startEnabled;
        mOnPressDown = onPressDown;
        mOnRelease = onRelease;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     * @param onHover callback to be called once the button is hovered by the cursor
     * @param offHover callback to be called once the button is no longer hovered by the cursor
     */
    public UIButton(
            String label,
            IButtonEvent onClick,
            IButtonEvent onPressDown,
            IButtonEvent onRelease,
            IButtonEvent onHover,
            IButtonEvent offHover) {
        this(label, onClick, onPressDown, onRelease);
        mOnHover = onHover;
        mOffHover = offHover;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     * @param onHover callback to be called once the button is hovered by the cursor
     * @param offHover callback to be called once the button is no longer hovered by the cursor
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(
            String label,
            IButtonEvent onClick,
            IButtonEvent onPressDown,
            IButtonEvent onRelease,
            IButtonEvent onHover,
            IButtonEvent offHover,
            boolean startEnabled) {
        this(label, onClick, onPressDown, onRelease);
        mIsEnabled = startEnabled;
        mOnHover = onHover;
        mOffHover = offHover;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     * @param onHover callback to be called once the button is hovered by the cursor
     * @param offHover callback to be called once the button is no longer hovered by the cursor
     * @param whileHover callback to be called every frame while the button is hovered
     */
    public UIButton(
            String label,
            IButtonEvent onClick,
            IButtonEvent onPressDown,
            IButtonEvent onRelease,
            IButtonEvent onHover,
            IButtonEvent offHover,
            IButtonEvent whileHover) {
        this(label, onClick, onPressDown, onRelease, onHover, offHover);
        mWhileHover = whileHover;
    }

    /**
     * Constructor for UIButton.
     *
     * @param label a text label to render inside the button
     * @param onClick callback to be called when the button is clicked
     * @param onPressDown callback to be called when the button gets pressed down
     * @param onRelease callback to be called when the button gets released
     * @param onHover callback to be called once the button is hovered by the cursor
     * @param offHover callback to be called once the button is no longer hovered by the cursor
     * @param whileHover callback to be called every frame while the button is hovered
     * @param startEnabled true if the button should react to clicks onStart.
     */
    public UIButton(
            String label,
            IButtonEvent onClick,
            IButtonEvent onPressDown,
            IButtonEvent onRelease,
            IButtonEvent onHover,
            IButtonEvent offHover,
            IButtonEvent whileHover,
            boolean startEnabled) {
        this(label, onClick, onPressDown, onRelease, onHover, offHover);
        mIsEnabled = startEnabled;
        mWhileHover = whileHover;
    }

    /** Enables the button - allows it to be clicked and removes the disabled colour. */
    public void enable() {
        if (!mIsEnabled) {
            mIsEnabled = true;
            if (mMaterial != null) {
                mMaterial.mColour.set(mRegularColour);
            }
        }
    }

    /** Disables the button from running on clicks - also adds a disabled overlay colour. */
    public void disable() {
        if (mIsEnabled) {
            mIsEnabled = false;
            if (mMaterial != null) {
                mRegularColour.lerp(mDisabledColour, 0.8f, mTmpLerp);
                mMaterial.mColour.set(mTmpLerp);
            }
        }
    }

    /** Update the appearance of the button. */
    private void updateAppearance() {
        mRegularColour = mAppearance.getRegularColour();
        mHoveredColour = mAppearance.getHoveredColour();
        mPressedColour = mAppearance.getPressedColour();
        mDisabledColour = mAppearance.getDisabledColour();

        mTransitionTime = mAppearance.getTransitionTime();

        if (mRectTexture == null) {
            mRectTexture = mAppearance.getButtonTexture().clone();
        }
    }

    @Override
    public void onAwake() {
        updateAppearance();

        super.onAwake();

        if (!mIsEnabled) {
            mRegularColour.lerp(mDisabledColour, 0.8f, mTmpLerp);
            mMaterial.mColour.set(mTmpLerp);
        }
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
        if (mIsEnabled) {
            boolean mouseDown = UI_PRESS.isActivated();

            // Call mOnRelease if we finally released the button
            if (!mouseDown && mPressedDown) {
                mPressedDown = false;
                if (mOnRelease != null) {
                    mOnRelease.eventHandler(this, deltaTime);
                }
            }

            if (mRenderable != null && UIManager.getInstance().getHoveredObject() == mRenderable) {

                // We moved the mouse over without pressing it down
                if (mHadReleasedHover) {
                    if (!mouseDown) {
                        // Call mOnClick if we pressed this button
                        if (mLastMouseDown) {
                            if (mAppearance.getOnClick() != null) {
                                mAppearance.getOnClick().eventHandler(this, deltaTime);
                            }
                            if (mOnClick != null) {
                                mOnClick.eventHandler(this, deltaTime);
                            }
                        }
                    } else if (!mLastMouseDown) {
                        mPressedDown = true;
                        // Call mOnPressDown if we pressed down the button
                        if (mAppearance.getOnPressDown() != null) {
                            mAppearance.getOnPressDown().eventHandler(this, deltaTime);
                        }
                        if (mOnPressDown != null) {
                            mOnPressDown.eventHandler(this, deltaTime);
                        }
                    }
                }

                // Handle cases where cursor enters/leaves the button while pressing the button down
                if (!mouseDown) {
                    mHadReleasedHover = true;
                } else if (!mHadReleasedHover) {
                    mouseDown = false;
                }

                if (!mLastHovered) {
                    if (mOnHover != null) {
                        mOnHover.eventHandler(this, deltaTime);
                    }
                }

                mLastHovered = true;
                if (mWhileHover != null) {
                    mWhileHover.eventHandler(this, deltaTime);
                }
            } else {
                if (mLastHovered) {
                    if (mOffHover != null) {
                        mOffHover.eventHandler(this, deltaTime);
                    }
                }

                mLastHovered = false;
                mHadReleasedHover = false;
            }

            // Transition color interpolation value depending on the state of button press
            if (mPressedDown || mLockPressed) {
                UIManager.setNextCursor(UIAppearance.getInClickCursor());
                mCurTimer += deltaTime;
                if (mCurTimer > 2f * mTransitionTime) {
                    mCurTimer = 2f * mTransitionTime;
                }
            } else if (mLastHovered) {
                UIManager.setNextCursor(UIAppearance.getHoverCursor());
                if (mCurTimer > mTransitionTime) {
                    mCurTimer -= deltaTime;
                    if (mCurTimer < mTransitionTime) {
                        mCurTimer = mTransitionTime;
                    }
                } else {
                    mCurTimer += deltaTime;
                    if (mCurTimer > mTransitionTime) {
                        mCurTimer = mTransitionTime;
                    }
                }
            } else {
                mCurTimer -= deltaTime;
                if (mCurTimer < 0.f) {
                    mCurTimer = 0.f;
                }
            }

            mLastMouseDown = mouseDown;

            if (mMaterial != null) {
                // Interpolate material colours to represent button click state
                if (mCurTimer > mTransitionTime) {
                    mHoveredColour.lerp(mPressedColour, mCurTimer / mTransitionTime - 1f, mTmpLerp);
                } else {
                    mRegularColour.lerp(mHoveredColour, mCurTimer / mTransitionTime, mTmpLerp);
                }
                mMaterial.mColour.set(mTmpLerp);
            }
        }
    }
}
