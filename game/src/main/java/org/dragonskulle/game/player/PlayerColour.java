/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Stores the various possible player colours.
 *
 * <p>When adding a colour, please ensure that it can be seen against the green tile, and is
 * distinctive enough from the other colours.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
enum PlayerColour {
    // BLACK(0, 0, 0),
    // GREY(64, 64, 64),
    RED(255, 0, 0),
    ORANGE(255, 106, 0),
    // YELLOW(255, 216, 0),
    LIGHT_BLUE(0, 255, 255),
    // SKY_BLUE(0, 148, 255),
    BLUE(0, 0, 255),
    // DARK_BLUE(0, 0, 128),
    DARK_PURPLE(72, 0, 255),
    PURPLE(178, 0, 255),
    // PINK(255, 0, 255),
    WHITE(255, 255, 255);

    /** The minimum normal RBG value. */
    private static final int sMin = 0;
    /** The maximum normal RBG value. */
    private static final int sMax = 255;

    /** The colour in RGB form, normalised between 0 and 1. */
    @Getter private final Vector3fc mColour;

    /**
     * Create a new colour able to be used to denote a player.
     *
     * @param red The red value, normally between 0 and 255.
     * @param green The green value, normally between 0 and 255.
     * @param blue The blue value, normally between 0 and 255.
     */
    PlayerColour(int red, int green, int blue) {
        mColour = normaliseRGB(red, green, blue);
    }

    /**
     * Normalise the RGB combination.
     *
     * @param red The red value.
     * @param green The green value.
     * @param blue The blue value.
     * @return A {@link Vector3f} containing versions of the RGB values, each normalised between 0
     *     and 1.
     */
    private Vector3f normaliseRGB(int red, int green, int blue) {
        float normalisedRed = MathUtils.normalise(red, sMin, sMax);
        float normalisedGreen = MathUtils.normalise(green, sMin, sMax);
        float normalisedBlue = MathUtils.normalise(blue, sMin, sMax);

        return new Vector3f(normalisedRed, normalisedGreen, normalisedBlue);
    }

    /**
     * Get a player colour at the given index.
     *
     * <ul>
     *   <li>If the index exceeds the number of colours, the list of colours will loop.
     *   <li>If the index is negative, the list of colours will start from the end.
     * </ul>
     *
     * @param index The index of the desired colour.
     * @return A {@link Vector3f} containing the colour, in RGB normalised between 0 and 1.
     */
    public static Vector3fc getColour(int index) {
        PlayerColour[] colours = values();
        PlayerColour selectedColour = colours[Math.floorMod(index, colours.length)];
        return selectedColour.getColour();
    }
}
