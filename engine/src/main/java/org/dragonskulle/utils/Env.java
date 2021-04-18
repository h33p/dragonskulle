/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

public class Env {
    public static String envString(String key, String defaultVal) {
        String envLine = System.getenv(key);
        return envLine == null ? defaultVal : envLine;
    }

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
