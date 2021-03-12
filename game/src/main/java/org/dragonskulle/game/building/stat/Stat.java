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

    /**
     * The current level of the stat. This will always be between {@link #LEVEL_MIN} and {@link
     * #LEVEL_MAX}, inclusive.
     */
    @Getter protected int mLevel = LEVEL_MIN;

    /**
     * Calculate the value from {@link #mLevel}.
     *
     * @return The value, of type {@code T}, of the stat at the current {@link #mLevel}.
     */
    protected abstract T levelToValue();

    /** Bound the level between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}. */
    protected void boundLevel() {
        if (mLevel < LEVEL_MIN) {
            mLevel = LEVEL_MIN;
        } else if (mLevel > LEVEL_MAX) {
            mLevel = LEVEL_MAX;
        }
    }

    /** Increase the level of the stat. */
    public void increaseLevel() {
        mLevel++;
        boundLevel();
    }

    /** Decrease the level of the stat. */
    public void decreaseLevel() {
        mLevel--;
        boundLevel();
    }

    /**
     * Set the level. This will be bound between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     * 
     * @param level The level.
     */
    public void setLevel(int level) {
    	mLevel = level;
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
    
    /**
     * Useful for mapping the level to between two doubles.
     * 
     * @param valueMin The lowest possible value of the stat.
     * @param valueMax The highest possible value of the stat.
     * @param level The current level.
     * @param levelMin The lowest possible level.
     * @param levelMax The highest possible level.
     * @return The value, between {@code valueMin} and {@code valueMax}, based on the specified {@code level}.
     */
    protected double map(double valueMin, double valueMax, double level, double levelMin, double levelMax) {
    	return valueMin + (((level - levelMin) * (valueMax - valueMin))/(levelMax - levelMin));
    }
}
