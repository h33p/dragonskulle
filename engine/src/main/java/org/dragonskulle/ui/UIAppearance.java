/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIButton.IButtonEvent;
import org.joml.Vector4f;
import org.lwjgl.system.NativeResource;

/**
 * Describes UI appearance and visual behaviour.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Getter
@Setter
public class UIAppearance implements NativeResource {
    /** Regular colour for a button. */
    private Vector4f mRegularColour = new Vector4f(1f);
    /** Hovered colour for a button. */
    private Vector4f mHoveredColour = new Vector4f(0.8f, 0.8f, 0.8f, 1f);
    /** The colour a button has when pressed down. */
    private Vector4f mPressedColour = new Vector4f(0.6f, 0.6f, 0.6f, 1f);
    /** The colour a button has when disabled. */
    private Vector4f mDisabledColour = new Vector4f(0.882f, 0.027f, 0.019f, 1f);
    /** How quickly colour transitions happen for buttons. */
    private float mTransitionTime = 0.1f;
    /** The font used by text. */
    private Resource<Font> mTextFont = Font.getFontResource("CascadiaCode.ttf");
    /** The colour text has. */
    private Vector4f mTextColour = new Vector4f(0f, 0f, 0f, 1f);
    /** How much of a button is used by its icon (on the left side). */
    private float mButtonIconSplit = 0.2f;
    /** Margins from parent box edges that are set for text. */
    private float mRectTextVertMargin = 0.05f;
    /** Margins from parent box edges that are set for text. */
    private float mRectTextHorizMargin = 0.05f;
    /** Size slider knobs have. */
    private float mSliderKnobSize = 10f;
    /** Height of a UI element in vertical UI. */
    private float mVerticalUIElemHeight = 0.07f;
    /** Width of a UI element in Horizontal UI. */
    private float mHorizontalUIElemWidth = 0.27f;
    /** Gap between UI elements in vertical UI. */
    private float mVerticalUIElemGap = 0.03f;
    /** Gap between UI elements in vertical UI. */
    private float mHorizUIElemGap = 0.03f;
    /** Texture of a regular button. */
    private SampledTexture mButtonTexture = new SampledTexture("ui/wide_button.png");
    /** Texture of a regular text rect. */
    private SampledTexture mTextRectTexture = new SampledTexture("ui/wide_button.png");
    /** Texture of a drop down menu icon. */
    private SampledTexture mDropDownIconTexture = new SampledTexture("ui/drop_down_icon.png");
    /** Texture of a slider knob. */
    private SampledTexture mSliderKnobTexture = new SampledTexture("ui/round_knob.png");
    /** Texture of a regular rectangle. */
    private SampledTexture[] mRectTextures = {new SampledTexture("white.bmp")};
    /** Injected onClick event to play sounds or something. */
    private IButtonEvent mOnClick = null;
    /** Injected onPressDown event to play sounds or something. */
    private IButtonEvent mOnPressDown = null;

    public void setTextFont(Resource<Font> textFont) {
        if (mTextFont != null) {
            mTextFont.free();
        }
        mTextFont = textFont;
    }

    public void setButtonTexture(SampledTexture buttonTexture) {
        if (mButtonTexture != null) {
            mButtonTexture.free();
        }
        mButtonTexture = buttonTexture;
    }

    public void setTextRect(SampledTexture textRectTexture) {
        if (mTextRectTexture != null) {
            mTextRectTexture.free();
        }
        mTextRectTexture = textRectTexture;
    }

    public void setDropDownIconTexture(SampledTexture dropDownIconTexture) {
        if (mDropDownIconTexture != null) {
            mDropDownIconTexture.free();
        }
        mDropDownIconTexture = dropDownIconTexture;
    }

    public void setSliderKnobTexture(SampledTexture sliderKnobTexture) {
        if (mSliderKnobTexture != null) {
            mSliderKnobTexture.free();
        }
        mSliderKnobTexture = sliderKnobTexture;
    }

    public void setRectTextures(SampledTexture[] rectTextures) {
        if (mRectTextures != null) {
            for (SampledTexture tex : mRectTextures) {
                tex.free();
            }
        }
        this.mRectTextures = rectTextures;
    }

    @Override
    public void free() {
        setTextFont(null);
        setButtonTexture(null);
        setTextRectTexture(null);
        setDropDownIconTexture(null);
        setSliderKnobTexture(null);
        setRectTextures(null);
    }
}
