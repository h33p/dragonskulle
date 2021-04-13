/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIAppearence;
import org.dragonskulle.ui.UIManager;

public class GameAppearence {

    public static final int INFO_BOX = 0;
    public static final int DRAWER = 1;

    public static SampledTexture getInfoBoxTexture() {
        return UIManager.getInstance().getAppearence().getRectTextures()[INFO_BOX].clone();
    }

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
    }
}
