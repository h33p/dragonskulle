/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import java.util.HashMap;

/**
 * Stores data elements that are either activated ({@code true}) or not activated ({@code false}).
 *
 * @author Craig Wilbourne
 * @param <T> The data type of the stored elements.
 */
public abstract class Activatable<T> {

    /** Stores elements that are either {@code true} or {@code false}. */
    private final HashMap<T, Boolean> mStored = new HashMap<T, Boolean>();

    /**
     * Set whether an element is activated.
     *
     * @param key The element.
     * @param value Whether the element should be activated.
     */
    void setActivated(T key, Boolean value) {
        mStored.put(key, value);
    }

    /**
     * Query if an element is activated.
     *
     * @param key The element.
     * @return If the element is activated return {@code true}, otherwise {@code false}.
     */
    boolean isActivated(T key) {
        if (key == null) {
            return false;
        }
        Boolean value = mStored.get(key);
        if (value == null) {
            return false;
        }
        return value.booleanValue();
    }
}
