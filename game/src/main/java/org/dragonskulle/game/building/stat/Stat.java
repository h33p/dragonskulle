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
}
