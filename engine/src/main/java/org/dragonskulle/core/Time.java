/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Class used for getting system time
 *
 * @author Harry Stoltz
 *      <p>
 *          Simple class to get the current time in seconds
 *      </p>
 */
public class Time {
    private static final double MS_PER_S = 1000.0;

    // This can either be done using nanoTime -> Seconds or as it currently is, not sure which is
    // better

    /**
     * Get the current system time in seconds
     *
     * @return The current time in seconds
     */
    static double getTimeInSeconds() {
        return (double)System.currentTimeMillis() / MS_PER_S;
    }
}
