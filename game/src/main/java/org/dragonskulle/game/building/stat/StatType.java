/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.game.GameConfig;
import org.dragonskulle.game.building.stat.SyncStat.IConfigChooser;

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
    ATTACK(GameConfig::getAttackStat),
    BUILD_DISTANCE(GameConfig::getBuildDistanceStat),
    CLAIM_DISTANCE(GameConfig::getClaimDistanceStat),
    DEFENCE(GameConfig::getDefenceStat),
    TOKEN_GENERATION(GameConfig::getGenerationStat),
    VIEW_DISTANCE(GameConfig::getViewDistanceStat);

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

    /** Configuration value receiver. */
    @Getter private final IConfigChooser mConfigChooser;

    /**
     * Create a new type of stat.
     *
     * @param configChooser The method used to pick the right config value from game config.
     */
    StatType(IConfigChooser configChooser) {
        mConfigChooser = configChooser;
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
