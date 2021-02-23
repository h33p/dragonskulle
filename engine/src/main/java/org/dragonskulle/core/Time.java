/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Class used for getting system time
 *
 * @author Harry Stoltz
 *     <p>Simple class to get the current time in seconds
 */
public class Time {
    private static final float NS_PER_S = 1000000000.f;

    /**
     * Get the current system time in seconds
     *
     * @return The current time in seconds
     */
    public static float getTimeInSeconds() {
        return ((float)System.nanoTime()) / NS_PER_S;
    }
}
