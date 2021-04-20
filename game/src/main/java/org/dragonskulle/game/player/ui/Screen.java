/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

/**
 * A {@code enum} which contains all the different screens the human player could see whilst playing
 * a game.
 *
 * @author DragonSkulle
 */
public enum Screen {
    MAP_SCREEN, // All the buildings you can see everything need to click a tile to interact
    BUILDING_SELECTED_SCREEN, // The actions you can take when you have clicked on your building
    BUILD_TILE_SCREEN, // The action to build a building on that tile.
    ATTACK_SCREEN, // The Screen to show what to show when user has clicked attack
    UPGRADE_SCREEN, // The screen to show which stat to show
    ATTACKING_SCREEN, // The screen to allow user to choose the building to attack
    SELLING_SCREEN, // The Screen to allow the user to confirm their sell.
    PLACING_NEW_BUILDING; // The screen to allow the user to place a pre-defined building
}
