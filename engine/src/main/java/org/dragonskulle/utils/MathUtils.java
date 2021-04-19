/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

import java.nio.ByteBuffer;

/**
 * Basic additional math utilities
 *
 * @author Aurimas Blažulionis
 */
public class MathUtils {

    public static final float DEG_TO_RAD = (float) Math.PI / 180.f;

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

    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
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
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n >> 1;
    }

    public static float roundStep(float val, float step) {
        return Math.round(val / step) * step;
    }

    public static double mapOneRangeToAnother(
            double sourceNumber,
            double fromA,
            double fromB,
            double toA,
            double toB,
            int decimalPrecision) {
        double deltaA = fromB - fromA;
        double deltaB = toB - toA;
        double scale = deltaB / deltaA;
        double negA = -1 * fromA;
        double offset = (negA * scale) + toA;
        double finalNumber = (sourceNumber * scale) + offset;
        int calcScale = (int) Math.pow(10, decimalPrecision);
        return (double) Math.round(finalNumber * calcScale) / calcScale;
    }

    /**
     * Normalise a value between 0 and 1, by providing the range the value is situated in.
     *
     * @param value The value to normalise.
     * @param minimum The lowest value the input could be.
     * @param maximum The highest value the input could be.
     * @return The value normalised between 0 and 1.
     */
    public static float normalise(float value, float minimum, float maximum) {
        return (float) mapOneRangeToAnother(value, minimum, maximum, 0, 1, 9);
    }

    /**
     * Converts RGB integer Array to a RGBA byte array. Adapted from
     * https://www.javaer101.com/en/article/46458013.html
     *
     * @param argb the ARGB array
     * @param height the height of the image
     * @param width the width of the image
     * @param dest the destination buffer
     */
    public static void intARGBtoByteRGBA(int[] argb, int height, int width, ByteBuffer dest) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];
                dest.put((byte) ((pixel >> 16) & 0xFF)); // red
                dest.put((byte) ((pixel >> 8) & 0xFF)); // green
                dest.put((byte) (pixel & 0xFF)); // blue
                dest.put((byte) ((pixel >> 24) & 0xFF)); // alpha
            }
        }
        dest.flip();
    }
}
