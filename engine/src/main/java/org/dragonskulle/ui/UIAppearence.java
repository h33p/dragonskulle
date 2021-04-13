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
 * Describes UI appearence and visual behaviour
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Getter
@Setter
public class UIAppearence implements NativeResource {
    /** Regular colour for a button */
    private Vector4f mRegularColour = new Vector4f(1f);
    /** Hovered colour for a button */
    private Vector4f mHoveredColour = new Vector4f(0.8f, 0.8f, 0.8f, 1f);
    /** The colour a button has when pressed down */
    private Vector4f mPressedColour = new Vector4f(0.6f, 0.6f, 0.6f, 1f);
    /** The colour a button has when disabled */
    private Vector4f mDisabledColour = new Vector4f(0.882f, 0.027f, 0.019f, 1f);
    /** How quickly colour transitions happen for buttons */
    private float mTransitionTime = 0.1f;
    /** The font used by text */
    private Resource<Font> mTextFont = Font.getFontResource("CascadiaCode.ttf");
    /** The colour text has */
    private Vector4f mTextColour = new Vector4f(0f, 0f, 0f, 1f);
    /** Margins from parent box edges that are set for text */
    private float mRectTextVertMargin = 0.05f;
    /** Margins from parent box edges that are set for text */
    private float mRectTextHorizMargin = 0.05f;
    /** Size slider knobs have */
    private float mSliderKnobSize = 10f;
    /** Texture of a regular button */
    private SampledTexture mButtonTexture = new SampledTexture("ui/wide_button.png");
    /** Texture of a slider knob */
    private SampledTexture mSliderKnobTexture = new SampledTexture("ui/round_knob.png");
    /** Texture of a regular rectangle */
    private SampledTexture[] mRectTextures = {new SampledTexture("white.bmp")};
    /** Injected onClick event to play sounds or something */
    private IButtonEvent mOnClick = null;
    /** Injected onPressDown event to play sounds or something */
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
        setSliderKnobTexture(null);
        setRectTextures(null);
    }
}
