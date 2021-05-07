/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;

/**
 * This allows players to be spawned by type. All the information about how to spawn the player is
 * inside this class.
 */
@Accessors(prefix = "m")
public class PlayerType {
    @Getter private final Component[] mComponent;
    private final PlayerStyle mPlayerStyle;

    /**
     * Gets if the Player is a human player or not.
     *
     * @return true if human, false otherwise
     */
    public boolean isHuman() {
        return mPlayerStyle.isHuman();
    }

    /** Types of player allowed to be created. */
    public static enum PlayerStyle {
        HUMAN(true),
        AIMER(false),
        PROBABILISTIC(false);

        @Accessors(prefix = "m")
        @Getter
        private final boolean mHuman;

        /**
         * Constructor.
         *
         * @param isHuman true if the Player is human, false otherwise
         */
        PlayerStyle(boolean isHuman) {
            mHuman = isHuman;
        }
    }

    /**
     * Constructor.
     *
     * @param playerStyle the player style
     * @param component the components to be spawned with this player type.
     */
    public PlayerType(PlayerStyle playerStyle, Component[] component) {
        mPlayerStyle = playerStyle;
        mComponent = component;
    }
}
