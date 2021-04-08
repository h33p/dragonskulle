/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import lombok.extern.java.Log;

@Log
public enum Stat {
    ATTACK,
    DEFENCE,
    TOKEN_GENERATION,
    VIEW_DISTANCE,
    ATTACK_DISTANCE;

    /** The index of the specific Stat in {@link #values()}. */
    private int mID;

    /** Set the IDs of the Stats. */
    static {
        int current = 0;
        for (Stat stat : values()) {
            stat.mID = current;
            current++;
        }
    }

    /**
     * Get the ID of the Stat.
     *
     * @return The ID.
     */
    public int getID() {
        return mID;
    }

    /**
     * Get a {@link Stat} from its ID.
     *
     * @param id The ID of the desired Stat.
     * @return The desired Stat, or {@code null}.
     */
    public static Stat getFromID(int id) {
        Stat[] values = values();
        if (id < 0 || id > values.length) {
            log.warning("Stat ID out of range: " + id);
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
    public static int getAttackValue(int level) {
        // The attack value is identical to the current level number plus one.
        return level + 1;
    }

    /**
     * Used as a lambda expression to get the value of a defence stat for a given level.
     *
     * @param level The level of the stat.
     * @return The stat's defence value.
     */
    public static int getDefenceValue(int level) {
        // The defence value is identical to the current level number plus one.
        return level + 1;
    }

    /**
     * Used as a lambda expression to get the token generation of a building for a given level.
     *
     * @param level The level of the stat.
     * @return The building's token generation at that level.
     */
    public static int getTokenGenerationValue(int level) {
        // The number of tokens to generate is identical to the current level number.
        return level;
    }

    /**
     * Used as a lambda expression to get the view distance of a building for a given level.
     *
     * @param level The level of the stat.
     * @return The building's view distance.
     */
    public static int getViewDistanceValue(int level) {
        // Regardless of the level, the value of the stat will always be 3.
        return 3;
    }

    /**
     * Used as a lambda expression to get the attack distance of a building for a given level.
     *
     * @param level The level of the stat.
     * @return The building's attack distance.
     */
    public static int getAttackDistanceValue(int level) {
        // Regardless of the level, the value of the attack distance will always be 2.
        return 2;
    }
}
