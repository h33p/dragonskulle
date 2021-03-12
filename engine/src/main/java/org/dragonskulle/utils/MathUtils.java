/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

/**
 * Basic additional math utilities
 *
 * @author Aurimas Blažulionis
 */
public class MathUtils {

    /**
     * Interpolate a float value between start and end with time
     *
     * @param start starting point that we get when time is 0
     * @param end ending point that we get when time is 1
     * @param time value we use to interpolate between start and end. If outside [0-1] range, the
     *     value will be extrapolated outside the bounds.
     * @return interpolated value
     */
    public static float lerp(float start, float end, float time) {
        return start + (end - start) * time;
    }
}
