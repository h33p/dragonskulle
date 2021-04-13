/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIAppearence;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;

/**
 * This class controls how UI looks throughout the game
 *
 * @author Aurimas Blažulionis
 */
public class GameUIAppearence {

    private static final int BUTTON_DOWN_SFX_ID =
            AudioManager.getInstance().loadSound("button-down.wav");
    private static final int BUTTON_SFX_ID = AudioManager.getInstance().loadSound("button-up.wav");

    public static final int INFO_BOX = 0;
    public static final int DRAWER = 1;

    /**
     * Get a information box texture
     *
     * @return info box texture. The reference is cloned, so this texture needs to be freed by the
     *     callee
     */
    public static SampledTexture getInfoBoxTexture() {
        return UIManager.getInstance().getAppearence().getRectTextures()[INFO_BOX].clone();
    }

    /**
     * Get a drawer texture
     *
     * @return info drawer texture. The reference is cloned, so this texture needs to be freed by
     *     the callee
     */
    public static SampledTexture getDrawerTexture() {
        return UIManager.getInstance().getAppearence().getRectTextures()[DRAWER].clone();
    }

    /**
     * Initialise the UI appearence
     *
     * <p>This method will set the game's UI settings to look consistent.
     */
    public static void initialise() {
        UIAppearence appearence = UIManager.getInstance().getAppearence();
        appearence.getTextFont().free();
        appearence.setTextFont(Font.getFontResource("fatpixel.ttf"));
        appearence.setButtonTexture(new SampledTexture("ui/wide_button_new.png"));
        appearence.setRectTextures(
                new SampledTexture[] {
                    new SampledTexture("ui/info_box.png"), new SampledTexture("ui/drawer.png")
                });
        appearence.setRectTextVertMargin(0.3f);
        appearence.setRectTextHorizMargin(0.1f);
        appearence.setOnClick(GameUIAppearence::onClick);
        appearence.setOnPressDown(GameUIAppearence::onPressDown);
    }

    /** Inject a sound to every button click */
    private static void onClick(UIButton button, float deltaTime) {
        AudioSource source = getSource();

        if (source != null) {
            source.playSound(BUTTON_SFX_ID);
        }
    }

    /** Inject a sound to every button down press */
    private static void onPressDown(UIButton button, float deltaTime) {
        AudioSource source = getSource();

        if (source != null) {
            source.playSound(BUTTON_DOWN_SFX_ID);
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
