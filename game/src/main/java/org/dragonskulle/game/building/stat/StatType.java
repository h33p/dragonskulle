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
    ATTACK(
            // The attack value is identical to the current level number.
            (level) -> {
                return level;
            }),

    DEFENCE(
            // The defence value is identical to the current level number.
            (level) -> {
                return level;
            }),

    TOKEN_GENERATION(
            // The number of tokens to generate is identical to the current level number minus one.
            (level) -> {
                int tokens = level - 1;
                if (tokens < 0) {
                    return 0;
                }
                return tokens;
            }),

    VIEW_DISTANCE(
            // Regardless of the level, the view distance will always be the same.
            (level) -> {
                return 3;
            }),

    ATTACK_DISTANCE(
            // Regardless of the level, the attack distance will always be the same.
            (level) -> {
                return 3;
            }),

    BUILD_DISTANCE(
            // Regardless of the level, the build distance will always be the same.
            (level) -> {
                return 2;
            }),
    
    CLAIM_DISTANCE(
            // Regardless of the level, the claim distance will always be the same.
            (level) -> {
                return 1;
            });;

    /** Set the IDs of the Stats. */
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
    @Getter private IValueCalculator mValueCalculator;

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
