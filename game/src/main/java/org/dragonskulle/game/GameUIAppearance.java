/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.ui.UIAppearance;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;

/**
 * This class controls how UI looks throughout the game.
 *
 * @author Aurimas Blažulionis
 */
public class GameUIAppearance {

    private static final String BUTTON_DOWN_SOUND = "button-down.wav";
    private static final String BUTTON_UP_SOUND = "button-up.wav";

    public static final int INFO_BOX = 0;
    public static final int DRAWER = 1;
    public static final int SQUARE_BUTTON = 2;
    public static final int INFO_BOX_2_1 = 3;

    /**
     * Get a information box texture.
     *
     * @return info box texture. The reference is cloned, so this texture needs to be freed by the
     *     callee
     */
    public static SampledTexture getInfoBoxTexture() {
        return UIManager.getInstance().getAppearance().getRectTextures()[INFO_BOX].clone();
    }

    /**
     * Get a drawer texture.
     *
     * @return info drawer texture. The reference is cloned, so this texture needs to be freed by
     *     the callee
     */
    public static SampledTexture getDrawerTexture() {
        return UIManager.getInstance().getAppearance().getRectTextures()[DRAWER].clone();
    }

    /**
     * Get a square button texture.
     *
     * @return square button texture. The reference is cloned, so this texture needs to be freed by
     *     the callee
     */
    public static SampledTexture getSquareButtonTexture() {
        return UIManager.getInstance().getAppearance().getRectTextures()[SQUARE_BUTTON].clone();
    }

    /**
     * Get a 2:1 info box texture.
     *
     * @return 2:1 info box texture. The reference is cloned, so this texture needs to be freed by
     *     the callee
     */
    public static SampledTexture getInfoBox21Texture() {
        return UIManager.getInstance().getAppearance().getRectTextures()[INFO_BOX_2_1].clone();
    }

    /*
     Initialise the UI appearance.

     <p>This method will set the game's UI settings to look consistent.
    */
    static {
        UIAppearance appearance = UIManager.getInstance().getAppearance();
        appearance.setTextFont(Font.getFontResource("fatpixel.ttf"));
        appearance.setButtonTexture(new SampledTexture("ui/wide_button_new.png"));
        appearance.setTextRectTexture(new SampledTexture("ui/info_box.png"));
        appearance.setDropDownIconTexture(new SampledTexture("ui/drop_down_icon_new.png"));
        appearance.setSliderKnobTexture(new SampledTexture("ui/slider_bar.png"));
        appearance.setRectTextures(
                new SampledTexture[] {
                    new SampledTexture("ui/info_box.png"),
                    new SampledTexture("ui/drawer.png"),
                    new SampledTexture(
                            "ui/square_button.png", new TextureMapping(TextureFiltering.NEAREST)),
                    new SampledTexture("ui/info_box_2_1.png")
                });
        appearance.setRectTextVertMargin(0.3f);
        appearance.setRectTextHorizMargin(0.15f);
        appearance.setOnClick(GameUIAppearance::onClick);
        appearance.setOnPressDown(GameUIAppearance::onPressDown);
    }

    /** Inject a sound to every button click. */
    private static void onClick(UIButton button, float deltaTime) {
        AudioSource source = getSource();

        if (source != null) {
            source.playSound(BUTTON_UP_SOUND);
        }
    }

    /** Inject a sound to every button down press. */
    private static void onPressDown(UIButton button, float deltaTime) {
        AudioSource source = getSource();

        if (source != null) {
            source.playSound(BUTTON_DOWN_SOUND);
        }
    }

    private static AudioSource getSource() {
        AudioSource singleton = Scene.getActiveScene().getSingleton(AudioSource.class);

        if (singleton == null) {
            Reference<AudioListener> listener = AudioManager.getInstance().getAudioListener();

            if (Reference.isValid(listener)) {
                singleton = new AudioSource();
                listener.get().getGameObject().addComponent(singleton);
                Scene.getActiveScene().registerSingleton(singleton);
                singleton.setVolume(0.5f);
            }
        }

        return singleton;
    }
}
