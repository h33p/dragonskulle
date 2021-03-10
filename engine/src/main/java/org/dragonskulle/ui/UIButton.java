/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Reference;
import org.dragonskulle.input.Action;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Class describing a interactive UI button
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIButton extends Component implements IOnAwake, IFrameUpdate {
    /** Simple interface describing button callback events */
    public interface IButtonEvent {
        /**
         * Method for handling the event
         *
         * @param button calling button
         * @param deltaTime forwarded deltaTime from frameUpdate
         */
        public void eventHandler(UIButton button, float deltaTime);
    }

    /** Input action that needs to be bound for UI button presses to function */
    public static final Action UI_PRESS = new Action("UI_PRESS");

    private Vector4fc mRegularColor = new Vector4f(1f);
    private Vector4fc mHoveredColor = new Vector4f(0.8f, 0.8f, 0.8f, 1f);
    private Vector4fc mPressedColor = new Vector4f(0.6f, 0.6f, 0.6f, 1f);

    private Vector4f mTmpLerp = new Vector4f(1f);

    private float mTransitionTime = 0.1f;

    private float curTimer = 0f;
    private Reference<UIRenderable> mRenderable;
    private UIMaterial mMaterial;

    private IButtonEvent mOnClick;
    private IButtonEvent mOnHover;
    private IButtonEvent mOffHover;
    private IButtonEvent mWhileHover;
    private boolean mLastMouseDown = false;
    private boolean mLastHovered = false;
    private boolean mHadReleasedHover = true;

    public UIButton() {}

    /**
     * Constructor for UIButton
     *
     * @param onClick callback to be called when the button is clicked
     */
    public UIButton(IButtonEvent onClick) {
        mOnClick = onClick;
    }

    /**
     * Constructor for UIButton
     *
     * @param onClick callback to be called when the button is clicked
     * @param onHover callback to be called once the button is hovered by the cursor
     */
    public UIButton(IButtonEvent onClick, IButtonEvent onHover) {
        this(onClick);
        mOnHover = onHover;
    }

    /**
     * Constructor for UIButton
     *
     * @param onClick callback to be called when the button is clicked
     * @param onHover callback to be called once the button is hovered by the cursor
     * @param offHover callback to be called once the button is no longer hovered by the cursor
     */
    public UIButton(IButtonEvent onClick, IButtonEvent onHover, IButtonEvent offHover) {
        this(onClick, onHover);
        mOffHover = offHover;
    }

    /**
     * Constructor for UIButton
     *
     * @param onClick callback to be called when the button is clicked
     * @param onHover callback to be called once the button is hovered by the cursor
     * @param offHover callback to be called once the button is no longer hovered by the cursor
     * @param whileHover callback to be called every frame while the button is hovered
     */
    public UIButton(
            IButtonEvent onClick,
            IButtonEvent onHover,
            IButtonEvent offHover,
            IButtonEvent whileHover) {
        this(onClick, onHover, offHover);
        mWhileHover = whileHover;
    }

    @Override
    public void onAwake() {
        mRenderable = getGameObject().getComponent(UIRenderable.class);
        if (mRenderable == null || !mRenderable.isValid()) {
            getGameObject().addComponent(new UIRenderable());
            mRenderable = getGameObject().getComponent(UIRenderable.class);
        }

        UIRenderable rend = mRenderable.get();

        if (rend != null) {
            if (rend.getMaterial() instanceof UIMaterial)
                mMaterial = (UIMaterial) rend.getMaterial();
        }
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
        if (mRenderable != null && UIManager.getInstance().getHoveredObject() == mRenderable) {

            boolean mouseDown = UI_PRESS.isActivated();

            // Call mOnClick if we pressed this button
            if (!mouseDown && mHadReleasedHover) {
                if (mLastMouseDown && mOnClick != null) mOnClick.eventHandler(this, deltaTime);
            }

            // Handle cases where cursor enters/leaves the button while pressing the button down
            if (!mouseDown) mHadReleasedHover = true;
            else if (!mHadReleasedHover) mouseDown = false;

            mLastMouseDown = mouseDown;

            // Transition color interpolation value depending on the state of button press
            if (!mouseDown && curTimer > mTransitionTime) {
                curTimer -= deltaTime;
                if (curTimer < mTransitionTime) curTimer = mTransitionTime;
                mLastMouseDown = false;
            } else {
                curTimer += deltaTime;
                if (curTimer > 2f * mTransitionTime) curTimer = 2f * mTransitionTime;
            }

            if (!mLastHovered && mOnHover != null) mOnHover.eventHandler(this, deltaTime);

            mLastHovered = true;

            if (mWhileHover != null) mWhileHover.eventHandler(this, deltaTime);
        } else {
            curTimer -= deltaTime;
            if (curTimer < 0.f) curTimer = 0.f;

            if (mLastHovered && mOffHover != null) mOffHover.eventHandler(this, deltaTime);

            mLastHovered = false;
            mLastMouseDown = false;
            mHadReleasedHover = false;
        }

        if (mMaterial != null) {
            // Interpolate material colours to represent button click state
            if (curTimer > mTransitionTime)
                mHoveredColor.lerp(mPressedColor, curTimer / mTransitionTime - 1f, mTmpLerp);
            else mRegularColor.lerp(mHoveredColor, curTimer / mTransitionTime, mTmpLerp);
            mMaterial.colour.set(mTmpLerp);
        }
    }
}
