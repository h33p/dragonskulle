/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
abstract class Stat<T> {

    /** The lowest level possible. */
    public static final int LEVEL_MIN = 0;
    /** The highest level possible. */
    public static final int LEVEL_MAX = 5;

    /** The current level of the stat. This will always be between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}, inclusive. */
    @Getter protected int mLevel = 0;

    /**
     * Calculate the value from {@link #mLevel}.
     * 
     * @return The value, of type {@code T}, of the stat at the current {@link #mLevel}.
     */
    protected abstract T levelToValue();

    /**
     * Bound the level between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     */
    protected void boundLevel() {
        if (mLevel < LEVEL_MIN) {
        	mLevel = LEVEL_MIN;
        } else if (mLevel > LEVEL_MAX) {
        	mLevel = LEVEL_MAX;
        }
    }

    /**
     * Increase the level of the stat.
     */
    public void increaseLevel() {
        mLevel++;
        boundLevel();
    }

    /**
     * Decrease the level of the stat.
     */
    public void decreaseLevel() {
        mLevel--;
        boundLevel();
    }

    /**
     * Get the value of the stat, at the current level.
     * 
     * @return The value of the stat.
     */
    public T getValue() {
        return levelToValue();
    }
}
