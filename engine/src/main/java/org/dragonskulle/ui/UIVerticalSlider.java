/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.input.Actions;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.utils.MathUtils;
import org.joml.*;

/**
 * Class describing a interactive UI slider
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIVerticalSlider extends Component implements IOnAwake, IFrameUpdate {
    /** Simple interface describing slider callback events */
    public interface ISliderValueEvent {
        /**
         * Method for handling the eventH
         *
         * @param slider calling slider
         * @param value the new value of the slider
         */
        public void eventHandler(UIVerticalSlider slider, float value);
    }

    private ISliderValueEvent mOnValueChange;
    /** Controls the current value of the slider */
    @Getter @Setter private float mValue = 0f;
    /** Controls the minimum value the slider can have */
    @Getter @Setter private float mMinValue = 0f;
    /** Controls the maximum value the slider can have */
    @Getter @Setter private float mMaxValue = 1f;
    /** Controls the rounding of slider (1f will round to whole numbers) */
    @Getter @Setter private float mRoundStep = 0f;

    private TransformUI mKnobTransform;
    private boolean mPressed = false;
    Vector3f mTmpCursorPos = new Vector3f();
    Vector3f mTmpCursorPos2 = new Vector3f();

    /** Constructor for {@link UIVerticalSlider} */
    public UIVerticalSlider() {}

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param value default value of the slider
     */
    public UIVerticalSlider(float value) {
        mValue = value;
    }

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param value default value of the slider
     * @param minValue minimum value of the slider
     * @param maxValue minimum value of the slider
     */
    public UIVerticalSlider(float value, float minValue, float maxValue) {
        mValue = value;
        mMinValue = minValue;
        mMaxValue = maxValue;
    }

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param value default value of the slider
     * @param minValue minimum value of the slider
     * @param maxValue minimum value of the slider
     * @param roundStep rounding of the slider values
     */
    public UIVerticalSlider(float value, float minValue, float maxValue, float roundStep) {
        this(value, minValue, maxValue);
        mRoundStep = roundStep;
    }

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param onValueChange event that gets called when the slider value changes
     */
    public UIVerticalSlider(ISliderValueEvent onValueChange) {
        mOnValueChange = onValueChange;
    }

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param value default value of the slider
     * @param onValueChange event that gets called when the slider value changes
     */
    public UIVerticalSlider(float value, ISliderValueEvent onValueChange) {
        this(value);
        mOnValueChange = onValueChange;
    }

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param value default value of the slider
     * @param minValue minimum value of the slider
     * @param maxValue minimum value of the slider
     * @param onValueChange event that gets called when the slider value changes
     */
    public UIVerticalSlider(
            float value, float minValue, float maxValue, ISliderValueEvent onValueChange) {
        this(value, minValue, maxValue);
        mOnValueChange = onValueChange;
    }

    /**
     * Constructor for {@link UIVerticalSlider}
     *
     * @param value default value of the slider
     * @param minValue minimum value of the slider
     * @param maxValue minimum value of the slider
     * @param roundStep rounding of the slider value
     * @param onValueChange event that gets called when the slider value changes
     */
    public UIVerticalSlider(
            float value,
            float minValue,
            float maxValue,
            float roundStep,
            ISliderValueEvent onValueChange) {
        this(value, minValue, maxValue, roundStep);
        mOnValueChange = onValueChange;
    }

    @Override
    public void onAwake() {
        UIAppearance appearance = UIManager.getInstance().getAppearance();

        getGameObject().getTransform(TransformUI.class).setTargetAspectRatio(1f);
        getGameObject()
                .buildChild(
                        "slider bar",
                        new TransformUI(false),
                        (bar) -> {
                            bar.addComponent(
                                    new UIRenderable(
                                            new Vector4f(0.5f), new SampledTexture("white.bmp")));
                            TransformUI barTransform = bar.getTransform(TransformUI.class);
                            barTransform.setParentAnchor(0f, 0f, 0f, 1f);
                            barTransform.setMargin(-0.012f, -1.4f, 0.012f, 1.4f);
                            bar.buildChild(
                                    "slider knob",
                                    new TransformUI(true),
                                    (knob) -> {
                                        knob.addComponent(
                                                new UIRenderable(
                                                        appearance.getSliderKnobTexture().clone()));
                                        mKnobTransform = knob.getTransform(TransformUI.class);
                                        mKnobTransform.setParentAnchor(0f, 0f, 0f, 0f);
                                        mKnobTransform.setMargin(-15f, -15f, 15f, 15f);
                                        knob.addComponent(
                                                new UIButton(
                                                        null,
                                                        this::buttonPressDown,
                                                        this::buttonRelease));
                                    });
                        });
    }

    private void buttonPressDown(UIButton button, float __) {
        // Extract the starting mouse offset so we "pin" the knob to the mouse
        Matrix4fc invMatrix = mKnobTransform.getInvWorldMatrix();

        Vector2fc cursorCoords = Actions.getCursor().getPosition();

        mTmpCursorPos.set(cursorCoords.x(), cursorCoords.y(), 0f);

        mTmpCursorPos.mulPosition(invMatrix);

        mPressed = true;
    }

    private void buttonRelease(UIButton button, float __) {
        mPressed = false;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
        if (mPressed) {
            // Does it look to you like I have any idea how to
            // do accurate retransformation in one step here?

            // First, let's figure out the delta from value 0
            mKnobTransform.setParentAnchor(0f, 0f, 0f, 0f);
            Matrix4fc invMatrix = mKnobTransform.getInvWorldMatrix();
            Vector2fc cursorCoords = Actions.getCursor().getPosition();
            mTmpCursorPos2.set(cursorCoords.x(), cursorCoords.y(), 0f);
            mTmpCursorPos2.mulPosition(invMatrix);
            mTmpCursorPos2.sub(mTmpCursorPos);
            float y1 = mTmpCursorPos2.y();
            // Second, figure out the delta from value 1
            mKnobTransform.setParentAnchor(0f, 1f, 0f, 1f);
            invMatrix = mKnobTransform.getInvWorldMatrix();
            mTmpCursorPos2.set(cursorCoords.x(), cursorCoords.y(), 0f);
            mTmpCursorPos2.mulPosition(invMatrix);
            mTmpCursorPos2.sub(mTmpCursorPos);
            float y2 = mTmpCursorPos2.y();
            // Now then, inverse lerp the position into the range
            // if x1 is 0, x2 is 1, value has to be...
            float newValue = 0f;
            if (y1 <= 0) newValue = 0f;
            else if (y2 >= 0f) newValue = 1f;
            else newValue = 1f - y2 / (y2 - y1);

            newValue = MathUtils.lerp(mMinValue, mMaxValue, newValue);

            if (mRoundStep > 0f) {
                newValue /= mRoundStep;
                newValue = (float) Math.floor(newValue + 0.5f) * mRoundStep;
            }

            if (newValue != mValue && mOnValueChange != null)
                mOnValueChange.eventHandler(this, newValue);

            mValue = newValue;
        }

        float anchorVal = (mValue - mMinValue) / (mMaxValue - mMinValue);

        mKnobTransform.setParentAnchor(0.5f, anchorVal, 0.5f, anchorVal);
    }
}
