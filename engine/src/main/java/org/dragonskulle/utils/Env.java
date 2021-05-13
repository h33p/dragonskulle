/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

/**
 * Extract values from environment.
 *
 * @author Aurimas Blažulionis
 */
public class Env {
    /**
     * Get a string from environment.
     *
     * @param key environment variable to get.
     * @param defaultVal default value in case the variable is undefined.
     * @return the environment variable, or the default value.
     */
    public static String envString(String key, String defaultVal) {
        String envLine = System.getenv(key);
        return envLine == null ? defaultVal : envLine;
    }

    /**
     * Get an integer from environment.
     *
     * @param key environment variable to get.
     * @param defaultVal default value in case the variable is undefined.
     * @return integer value of the environment variable, or the default value.
     */
    public static int envInt(String key, int defaultVal) {
        String envLine = System.getenv(key);
        if (envLine == null) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(envLine);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * Get a boolean from environment.
     *
     * @param key environment variable to get.
     * @param defaultVal default value in case the variable is undefined.
     * @return boolean value of the environment variable, or the default value.
     */
    public static boolean envBool(String key, boolean defaultVal) {
        String envLine = System.getenv(key);
        if (envLine == null) {
            return defaultVal;
        }
        try {
            return Boolean.parseBoolean(envLine);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
