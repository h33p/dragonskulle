/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
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
@Log
public class SyncStat extends SyncInt {

    /** The lowest level possible. */
    public static final int LEVEL_MIN = 1;
    /** The highest level possible. */
    public static final int LEVEL_MAX = 5;

    /** Stores the building the stat is related to. */
    private Reference<Building> mBuilding = new Reference<Building>(null);

    /** An interface for getting the value of a stat at a given level. */
    public static interface IValueCalculator extends Serializable {
        int getValue(int level);
    }

    /** Store the function used to calculate the value of the stat. */
    private IValueCalculator mValueCalculator;

    /**
     * Create a new SyncStat, providing the method that will be used to calculate the value of the
     * stat for given levels, and the {@link Building} the stat relates to.
     *
     * @param valueCalculator The {@link IValueCalculator} used to calculate the value of the stat.
     * @param building The Building the stat relates to.
     */
    public SyncStat(IValueCalculator valueCalculator, Building building) {
        mValueCalculator = valueCalculator;
        mBuilding = building.getReference(Building.class);
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
     * @return The value of the stat, or {@code -1} on error.
     */
    public int getValue() {
        if (mValueCalculator == null) {
            log.warning("mValueCalculator is null.");
            return -1;
        }
        return mValueCalculator.getValue(getLevel());
    }

    @Override
    public void deserialize(DataInputStream in) throws IOException {
        super.deserialize(in);

        // The stats have changed, so call the building's afterStatChange.
        if (mBuilding == null || mBuilding.isValid() == false) return;
        mBuilding.get().afterStatChange();
    }
}
