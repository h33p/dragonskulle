/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import java.io.DataInputStream;
import java.io.IOException;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.network.components.sync.SyncInt;

/**
 * Stores a level and calculates the stat's value from this.
 *
 * <p>This level value is synchronised with the server.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
public abstract class SyncStat extends SyncInt {

    /** The lowest level possible. */
    public static final int LEVEL_MIN = 0;
    /** The highest level possible. */
    public static final int LEVEL_MAX = 5;

    private Reference<Building> mBuilding = new Reference<Building>(null);

    public SyncStat(Building building) {
        this.mBuilding = building.getReference(Building.class);
    }

    /**
     * @deprecated Please use #getLevel().
     *     <p>Get the stat's current level.
     * @return The level.
     */
    @Override
    public int get() {
        return super.get();
    }

    /**
     * Get the stat's current level.
     *
     * <p>This level will be between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     *
     * @return The level.
     */
    public int getLevel() {
        return super.get();
    }

    /**
     * Set the level, and calculate and the new value.
     *
     * <p>The level will be bound between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     *
     * @param level The level.
     */
    public void setLevel(int level) {
        level = getBoundedLevel(level);
        set(level);
    }

    /** Increase the level of the stat and calculate the new value. */
    public void increaseLevel() {
        int level = getLevel() + 1;
        setLevel(level);
    }

    /** Decrease the level of the stat and calculate the new value. */
    public void decreaseLevel() {
        int level = getLevel() - 1;
        setLevel(level);
    }

    /**
     * Bound the input level value between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     *
     * @param level The level value.
     * @return The level, bounded between the minimum and maximum possible levels.
     */
    private int getBoundedLevel(int level) {
        if (getLevel() < LEVEL_MIN) {
            return LEVEL_MIN;
        } else if (getLevel() > LEVEL_MAX) {
            return LEVEL_MAX;
        }
        return level;
    }

    /**
     * Get the value of the stat at the current level.
     *
     * @return The value of the stat.
     */
    public abstract int getValue();

    @Override
    public void deserialize(DataInputStream in) throws IOException {
        super.deserialize(in);

        // The stats have changed, so call the building's afterStatChange.
        if (mBuilding == null || mBuilding.isValid() == false) return;
        mBuilding.get().afterStatChange();
    }
}
