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

    // Regardless of the level, the view distance will always be 3.
    VIEW_DISTANCE(3),

    // Regardless of the level, the attack distance will always be 3.
    ATTACK_DISTANCE(3);

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

    /** Whether the stat always returns a fixed value. */
    @Getter private final boolean mFixedValue;

    /**
     * Create a new type of stat.
     *
     * @param valueCalculator The method used to turn a level into a value.
     */
    StatType(IValueCalculator valueCalculator) {
        mFixedValue = false;
        mValueCalculator = valueCalculator;
    }

    /**
     * Create a new type of stat that is permanently one value.
     *
     * @param value The value of the stat, regardless of level.
     */
    StatType(int value) {
        mFixedValue = true;
        mValueCalculator =
                (__) -> {
                    return value;
                };
    }

    public static StatType valueFromNiceName(String selectedOption) {
        switch (selectedOption) {
            case "Token Rate":
                return StatType.TOKEN_GENERATION;
            case "Defence Factor":
                return StatType.DEFENCE;
            case "Attack Factor":
                return StatType.ATTACK;
            default:
                return StatType.valueOf(selectedOption);
        }
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

    public String getNiceName() {
        String name = name();
        switch (name) {
            case "TOKEN_GENERATION":
                return "Token Rate";
            case "ATTACK":
                return "Attack Factor";
            case "DEFENCE":
                return "Defence Factor";
            default:
                return name;
        }
    }
}
