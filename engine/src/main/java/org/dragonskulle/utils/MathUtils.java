/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
     *  Generates a random weighted list summing to 1.
     *
     * @param numberOfDraws the number of draws
     * @param weights the corresponding weights
     * @return the list
     */
    public static List<Float> n_w_random(int numberOfDraws, int[] weights) {
        List<Integer> result = n_w_random(100, numberOfDraws, weights);
        return result.stream().map(s -> (s / 100f)).collect(Collectors.toList());
    }

    public static List<Integer> n_w_random(int targetSum, int numberOfDraws, int[] weights) {
        assert (numberOfDraws == weights.length);
        Random r = new Random();
        List<Integer> finalResult = new ArrayList<>();
        List<Integer> result = n_random(r, targetSum, Arrays.stream(weights).sum());
        for (int w : weights) {
            int sum = 0;
            for (int i = 0; i < w; i++) {
                sum += result.remove(r.nextInt(result.size()));
            }
            finalResult.add(sum);
        }
        return finalResult;
    }

    /**
     * Generate an array of floats that sum to targetSum. Adapted from
     * https://stackoverflow.com/a/22381217
     *
     * @param r Random Object
     * @param targetSum the target sum
     * @param numberOfDraws the number of numbers generated
     * @return the list
     */
    public static List<Integer> n_random(Random r, int targetSum, int numberOfDraws) {
        List<Integer> load = new ArrayList<>();
        // random numbers
        int sum = 0;
        for (int i = 0; i < numberOfDraws; i++) {
            int next = r.nextInt(targetSum) + 1;
            load.add(next);
            sum += next;
        }

        // scale to the desired target sum
        double scale = 1d * targetSum / sum;
        sum = 0;
        for (int i = 0; i < numberOfDraws; i++) {
            load.set(i, (int) (load.get(i) * scale));
            sum += load.get(i);
        }

        // take rounding issues into account
        while (sum++ < targetSum) {
            int i = r.nextInt(numberOfDraws);
            load.set(i, load.get(i) + 1);
        }
        return load;
    }
}
