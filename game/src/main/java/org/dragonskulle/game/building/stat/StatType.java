/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.game.building.stat.SyncStat.IValueCalculator;

/**
 * Stores different types of stats.
 *
 * <p>Each stat has:
 *
 * <ul>
 *   <li>An ID (its index in {@link #values()}).
 *   <li>A method to calculate its value for a given level.
 * </ul>
 *
 * @author Craig Wilbourne
 */
@Log
@Accessors(prefix = "m")
public enum StatType {
    ATTACK((level) -> level), // The attack value is identical to the current level number.
    ATTACK_DISTANCE((level) -> 3), // Regardless of the level, the attack distance will always be 3.
    DEFENCE((level) -> level), // The defence value is identical to the current level number.
    TOKEN_GENERATION(
            (level) ->
                    Math.max(
                            level - 1,
                            0)), // The number of tokens to generate is identical to the current
    // level number minus one.
    VIEW_DISTANCE((level) -> 3); // Regardless of the level, the view distance will always be 3.

    /* Set the IDs of the Stats. */
    static {
        int current = 0;
        for (StatType statType : values()) {
            statType.mID = current;
            current++;
        }
    }

    /** The index of the specific StatType in {@link #values()}. */
    private int mID;

    /** The method used to turn a level ({@code int}) into a value ({@code int}). */
    @Getter private final IValueCalculator mValueCalculator;

    /**
     * Instantiates a new Stat type.
     *
     * @param valueCalculator the calculator for the stat.
     */
    StatType(IValueCalculator valueCalculator) {
        mValueCalculator = valueCalculator;
    }

    /**
     * Get the ID of the StatType.
     *
     * @return The ID.
     */
    public int getID() {
        return mID;
    }

    /**
     * Get a {@link StatType} from its ID.
     *
     * @param id The ID of the desired StatType.
     * @return The desired StatType, or {@code null}.
     */
    public static StatType getFromID(int id) {
        StatType[] values = values();
        if (id < 0 || id > values.length) {
            log.warning("StatType ID out of range: " + id);
            return null;
        }

        return values[id];
    }
}
