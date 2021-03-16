/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import com.google.common.io.Resources;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.ISyncVar;
import org.dragonskulle.network.components.sync.SyncInt;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

@Accessors(prefix = "m")
public abstract class Stat<T> extends NetworkableComponent {

    /**
     * The lowest level possible.
     */
    public static final int LEVEL_MIN = 0;
    /**
     * The highest level possible.
     */
    public static final int LEVEL_MAX = 5;

    /**
     * The current level of the stat. This will always be between {@link #LEVEL_MIN} and {@link
     * #LEVEL_MAX}, inclusive.
     */
    @Getter
    protected SyncInt mLevel = new SyncInt(LEVEL_MIN);

    /**
     * Calculate the value from {@link #mLevel}.
     *
     * @return The value, of type {@code T}, of the stat at the current {@link #mLevel}.
     */
    protected abstract T levelToValue();

    /**
     * Bound the level between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     */
    protected void boundLevel(int newLevel) {
        if (newLevel < LEVEL_MIN) {
            mLevel.set(LEVEL_MIN);
        } else mLevel.set(Math.min(newLevel, LEVEL_MAX));
    }

    /**
     * Increase the level of the stat.
     */
    public void increaseLevel() {
        boundLevel(mLevel.get() + 1);
    }

    /**
     * Decrease the level of the stat.
     */
    public void decreaseLevel() {
        boundLevel(mLevel.get() - 1);
    }

    /**
     * Set the level. This will be bound between {@link #LEVEL_MIN} and {@link #LEVEL_MAX}.
     * This should only be called by the server
     *
     * @param level The level.
     */
    public void setLevel(int level) {
        boundLevel(level);
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
     * @param level    The current level.
     * @param levelMin The lowest possible level.
     * @param levelMax The highest possible level.
     * @return The value, between {@code valueMin} and {@code valueMax}, based on the specified
     * {@code level}.
     */
    protected double map(
            double valueMin, double valueMax, double level, double levelMin, double levelMax) {
        return valueMin + (((level - levelMin) * (valueMax - valueMin)) / (levelMax - levelMin));
    }
}
