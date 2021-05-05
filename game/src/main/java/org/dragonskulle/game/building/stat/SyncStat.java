/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
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
    public static final int LEVEL_MAX = 10;

    /** The cost of upgrading a stat if there is an error. */
    private static final int sErrorCost = 9999;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SyncStat syncStat = (SyncStat) o;
        return mType == syncStat.mType && getValue() == syncStat.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mType);
    }

    /** An interface for getting the value of a stat at a given level. */
    public static interface IValueCalculator extends Serializable {
        int getValue(int level);
    }

    /** Store the function used to calculate the value of the stat. */
    private IValueCalculator mValueCalculator;

    /** Store the type of the stat for ease of access. */
    @Getter private StatType mType;

    /** Stores the building the stat is related to. */
    private Reference<Building> mBuilding = new Reference<Building>(null);

    /**
     * Create a new SyncStat, providing the method that will be used to calculate the value of the
     * stat for given levels, and the {@link Building} the stat relates to.
     *
     * @param building The Building the stat relates to.
     */
    public SyncStat(Building building) {
        mBuilding = building.getReference(Building.class);

        // Initialise the level to the minimum value.
        setLevel(LEVEL_MIN);
    }

    /**
     * Initialise the stat by providing a {@link StatType}. The stat will take on the properties of
     * the specified StatType.
     *
     * @param type The type of stat to be.
     */
    public void initialise(StatType type) {
        // Store the type.
        mType = type;
        // Get the method used for calculating value.
        mValueCalculator = type.getValueCalculator();
    }

    /**
     * Get the value of the stat at the current level.
     *
     * @return The value of the stat, or {@code -1} on error.
     */
    public int getValue() {
        if (mValueCalculator == null) {
            log.warning("mValueCalculator is null.");
            return 0;
        }
        return mValueCalculator.getValue(getLevel());
    }

    /**
     * Get the cost of increasing this stat.
     *
     * @return The cost; otherwise {@value #sErrorCost}.
     */
    public int getCost() {
        if (!Reference.isValid(mBuilding)) {
            return sErrorCost;
        }

        return (getLevel() * 3) + mBuilding.get().getStatBaseCost();
    }

    /**
     * Get whether the stat is able to be upgraded at the current time.
     *
     * <p>Will be {@code false} if:
     *
     * <ul>
     *   <li>It is impossible to upgrade the stat (as its value is fixed).
     *   <li>The current level is equal to {@link #LEVEL_MAX}.
     * </ul>
     *
     * @return {@code true} if the stat is able to be further upgraded; otherwise {@code false}.
     */
    public boolean isUpgradeable() {
        return !(isFixed() || isMaxLevel());
    }

    /**
     * Checks if the Stat is a fixed value.
     *
     * @return true if fixed
     */
    public boolean isFixed() {
        return mType.isFixedValue();
    }

    /**
     * Checks if the Stat is equal to it's maximum level.
     *
     * @return true if equal
     */
    public boolean isMaxLevel() {
        return getLevel() == LEVEL_MAX;
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

    @Override
    public void deserialize(DataInputStream in) throws IOException {
        super.deserialize(in);

        // The stats have changed, so call the building's afterStatChange.
        if (!Reference.isValid(mBuilding)) {
            return;
        }
        mBuilding.get().afterStatChange(this.getType());
    }

    /**
     * @return The level.
     * @deprecated Please use #getLevel() for clarity.
     *     <p>Get the stat's current level.
     */
    @Override
    public int get() {
        return getLevel();
    }
}
