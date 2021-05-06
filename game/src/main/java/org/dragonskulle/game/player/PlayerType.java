/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;

@Accessors(prefix = "m")
public class PlayerType {
    private final PlayerStyle mPlayerStyle;

    public boolean isHuman() {
        return mPlayerStyle.isHuman();
    }

    public static enum PlayerStyle {
        HUMAN(true),
        AIMER(false),
        PROBABILISTIC(false);

        @Accessors(prefix = "m")
        @Getter
        private final boolean mHuman;

        PlayerStyle(boolean isHuman) {
            mHuman = isHuman;
        }
    }

    @Getter private final Component[] mComponent;

    public PlayerType(PlayerStyle playerStyle, Component[] component) {
        mPlayerStyle = playerStyle;
        mComponent = component;
    }
}
