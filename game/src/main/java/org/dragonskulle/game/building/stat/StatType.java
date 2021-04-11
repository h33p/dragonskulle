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
    ATTACK(StatType::getAttackValue),
    DEFENCE(StatType::getDefenceValue),
    TOKEN_GENERATION(StatType::getTokenGenerationValue),
    VIEW_DISTANCE(StatType::getViewDistanceValue),
    ATTACK_DISTANCE(StatType::getAttackDistanceValue);

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

    /**
     * Used as a lambda expression to get the value of an attack stat for a given level.
     *
     * @param level The level of the stat.
     * @return The stat's attack value.
     */
    private static int getAttackValue(int level) {
        // The attack value is identical to the current level number plus one.
        return level + 1;
    }

    /**
     * Used as a lambda expression to get the value of a defence stat for a given level.
     *
     * @param level The level of the stat.
     * @return The stat's defence value.
     */
    private static int getDefenceValue(int level) {
        // The defence value is identical to the current level number plus one.
        return level + 1;
    }

    /**
     * Used as a lambda expression to get the token generation of a building for a given level.
     *
     * @param level The level of the stat.
     * @return The building's token generation at that level.
     */
    private static int getTokenGenerationValue(int level) {
        // The number of tokens to generate is identical to the current level number.
        return level;
    }

    /**
     * Used as a lambda expression to get the view distance of a building for a given level.
     *
     * @param level The level of the stat.
     * @return The building's view distance.
     */
    private static int getViewDistanceValue(int level) {
        // Regardless of the level, the value of the stat will always be 3.
        return 3;
    }

    /**
     * Used as a lambda expression to get the attack distance of a building for a given level.
     *
     * @param level The level of the stat.
     * @return The building's attack distance.
     */
    private static int getAttackDistanceValue(int level) {
        // Regardless of the level, the value of the attack distance will always be 2.
        return 2;
    }
}
