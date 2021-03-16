/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import java.io.Serializable;

import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.GenericSync;
import org.dragonskulle.network.components.sync.SyncInt;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public abstract class Stat<T extends Serializable> extends NetworkableComponent {

    /** The lowest level possible. */
    public static final int LEVEL_MIN = 0;
    /** The highest level possible. */
    public static final int LEVEL_MAX = 5;

    /**
     * The current level of the stat. This will always be between {@link #LEVEL_MIN} and {@link
     * #LEVEL_MAX}, inclusive.
     */
    @Getter protected SyncInt mLevel = new SyncInt(LEVEL_MIN);
    
    /**
     * The value of the stat at the current level.
     */
    private SyncObject mValue = new SyncObject(getValueFromLevel());
    
    /** Used to sync the value of data type T. */
    private class SyncObject extends GenericSync<T> {

		public SyncObject(T data) {
			super(data);
		}
    	
    }
    
    /**
     * Set the level, and calculate and the new value.
     * <p>
     * The level will be bound between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     *
     * @param level The level.
     */
    public void setLevel(int level) {
        level = getBoundedLevel(level);
    	mLevel.set(level);
    	mValue.set(getValueFromLevel());
    }
    
    /** Increase the level of the stat and calculate the new value. */
    public void increaseLevel() {
        int level = mLevel.get() + 1;
        setLevel(level);
    }

    /** Decrease the level of the stat and calculate the new value. */
    public void decreaseLevel() {
    	int level = mLevel.get() - 1;
    	setLevel(level);
    }
    
    /**
     * Bound the input level value between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     * 
     * @param level The level value.
     * @return The level, bounded between the minimum and maximum possible levels.
     */
    private int getBoundedLevel(int level) {
        if (mLevel.get() < LEVEL_MIN) {
            return LEVEL_MIN;
        } else if (mLevel.get() > LEVEL_MAX) {
        	return LEVEL_MAX;
        }
        return level;
    }
    
    /**
     * Calculate the value of the stat from {@link #mLevel}.
     *
     * @return The value, of type {@code T}, of the stat at the current {@link #mLevel}.
     */
    protected abstract T getValueFromLevel();
    
    /**
     * Get the value stored in {@link #mValue}.
     * 
     * @return The value of the stat.
     */
    public T getValue() {
    	return mValue.get();
    }
    
}
