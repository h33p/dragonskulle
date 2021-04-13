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

public class GameUIAppearence {

    private static final int BUTTON_SFX_ID = AudioManager.getInstance().loadSound("button-10.wav");

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
        appearence.setOnClick(GameUIAppearence::onClick);
    }

    /** Inject a sound to every button click */
    private static void onClick(UIButton button, float deltaTime) {
        AudioSource singleton = Scene.getActiveScene().getSingleton(AudioSource.class);

        if (singleton == null) {
            Reference<AudioListener> listener = AudioManager.getInstance().getAudioListener();

            if (Reference.isValid(listener)) {
                singleton = new AudioSource();
                listener.get().getGameObject().addComponent(singleton);
                Scene.getActiveScene().registerSingleton(singleton);
                singleton.setVolume(0.1f);
            }
        }

        if (singleton != null) {
            singleton.playSound(BUTTON_SFX_ID);
        }
    }
}
