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

    /**
     * Calculate a logarithm in specified base
     *
     * @param val value to get the logarithm of
     * @param base base to calculate the logarithm in
     */
    public static int log(int val, int base) {
        return (int) (Math.log(val) / Math.log(base));
    }

    /**
     * Round a number down to the lower power of two.
     *
     * @param n number to round down
     * @return rounded down number
     */
    public static int roundDownToPow2(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n >> 1;
    }
}
