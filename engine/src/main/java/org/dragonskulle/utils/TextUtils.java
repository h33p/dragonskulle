/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

/**
 * Basic functions used to create Strings.
 *
 * @author Craig Wilbourne
 */
public class TextUtils {

    /**
     * Get a set amount of space characters, used for padding.
     *
     * @param length The number of spaces.
     * @return A String containing the desired number of spaces.
     */
    private static String getPadding(int length) {
        String output = "";
        for (int i = 0; i < length; i++) {
            output += " ";
        }
        return output;
    }

    /**
     * Add padding spaces to the end of a string.
     *
     * <p>If the input is larger than the desired length, no padding will be added.
     *
     * @param input The string to pad.
     * @param length The desired length.
     * @return The string padded with spaces so it meets the desired length.
     */
    public static String padAfter(String input, int length) {
        if (input == null || length <= 0) return input;

        final int additional = length - input.length();
        if (additional <= 0) return input;

        final String output = input + getPadding(additional);
        return output;
    }

    /**
     * Add padding spaces to the start of a string.
     *
     * <p>If the input is larger than the desired length, no padding will be added.
     *
     * @param input The string to pad.
     * @param length The desired length.
     * @return The string padded with spaces so it meets the desired length.
     */
    public static String padBefore(String input, int length) {
        if (input == null || length <= 0) return input;

        final int additional = length - input.length();
        if (additional <= 0) return input;

        final String output = getPadding(additional) + input;
        return output;
    }

    /**
     * Construct a String that contains the field's name and value, with padding added between the
     * values.
     *
     * <p>Will be formatted like "NAME: VALUE", with padding added to meet the desired length.
     *
     * @param name The name of the field.
     * @param value The value.
     * @param length The desired length of the output.
     * @return The name and value, with a colon and padding added.
     */
    public static String constructField(String name, String value, int length) {
        final String format = "%s: %s";

        // The text as it currently is.
        final String initialText = String.format(format, name, value);
        final int requiredPadding = length - initialText.length();

        final String paddedValue = padBefore(value, requiredPadding);
        return String.format(format, name, paddedValue);
    }

    /**
     * Construct a String that contains the field's name and value, with padding added between the
     * values.
     *
     * <p>Will be formatted like "NAME: VALUE", with padding added to meet the desired length.
     *
     * @param name The name of the field.
     * @param value The value as an int.
     * @param length The desired length of the output.
     * @return The name and value, with a colon and padding added.
     */
    public static String constructField(String name, int value, int length) {
        return constructField(name, Integer.toString(value), length);
    }
}
