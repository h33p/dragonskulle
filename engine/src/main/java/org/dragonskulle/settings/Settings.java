/* (C) 2021 DragonSkulle */
package org.dragonskulle.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * This class can be used to load settings from the settings file.
 *
 * @author Oscar L For loading in any settings from the settings.json file.
 */
@Accessors(prefix = "m")
@Log
public class Settings {
    @Getter(AccessLevel.PUBLIC)
    @Accessors(prefix = "s")
    private static final Settings sInstance = new Settings();

    @Accessors(prefix = "s")
    private static boolean sIsLoaded = false;

    private HashMap mSettings = new HashMap<>();
    private String mFilePath;
    private final String mDefaultFilePath = "settings.json";

    /** Singleton constructor. */
    private Settings() {}

    /**
     * Loads settings from the default location. root/settings.json
     *
     * @return
     */
    public Settings loadSettings() {
        loadSettings(mDefaultFilePath);
        return getInstance();
    }

    /**
     * Load settings into the singleton by file path.
     *
     * @param filePath the settings file path
     */
    void loadSettings(String filePath) {
        try {
            mFilePath = filePath;
            File sFile = new File(filePath);
            if (sFile.exists()) {
                mSettings = new ObjectMapper().readValue(sFile, HashMap.class);
                log.info("Loaded Settings");
                sIsLoaded = true;
            } else {
                log.warning("failed to load settings file");
            }
        } catch (IOException e) {
            log.warning("failed to load settings file, reason: " + e.getMessage());
        }
    }

    /**
     * Saves a key value pair in the settings without saving to disk.
     *
     * @param <T> the type of the value to be saved
     * @param name the name
     * @param value the value
     */
    public <T> void saveValue(String name, T value) {
        saveValue(name, value, false);
    }

    /**
     * Saves a key value pair in the settings. If {@code saveFile} is true we will also save to
     * disk.
     *
     * @param <T> the type of the value to be saved
     * @param name the name
     * @param value the value
     * @param saveFile true if we should save to the physical file
     */
    public <T> void saveValue(String name, T value, boolean saveFile) {
        mSettings.put(name, value.toString());
        if (saveFile) {
            save();
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return null.
     *
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the float setting value.
     */
    @SuppressWarnings("unchecked")
    public Float retrieveFloat(String name) {
        try {
            if (sIsLoaded) {
                Object val = mSettings.getOrDefault(name, null);
                if (val == null) return null;
                return Float.parseFloat(val.toString());
            } else {
                log.warning("Failed to read setting as not loaded.");
                return null;
            }
        } catch (Exception e) {
            log.warning("Failed to parse as a float, maybe it isn't one?");
            log.warning(e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param defaultValue the value returned if failed to retrieve
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the float setting value.
     */
    public Float retrieveFloat(String name, Float defaultValue) {
        Float value = retrieveFloat(name);
        if (value == null) {
            recreateSettingsFile(name, defaultValue);
        }
        return (value != null ? value : defaultValue);
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return null.
     *
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the string setting value.
     */
    @SuppressWarnings("unchecked")
    public String retrieveString(String name) {
        try {
            if (sIsLoaded) {
                Object val = mSettings.getOrDefault(name, null);
                if (val == null) return null;
                return val.toString();
            } else {
                log.warning("Failed to read setting as not loaded.");
                return null;
            }
        } catch (Exception e) {
            log.warning("Failed to parse as a string, maybe it isn't one?");
            return null;
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param defaultValue the value returned if failed to retrieve
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the String setting value.
     */
    public String retrieveString(String name, String defaultValue) {
        String value = retrieveString(name);
        if (value == null) {
            recreateSettingsFile(name, defaultValue);
        }
        return (value != null ? value : defaultValue);
    }

    private <T> void recreateSettingsFile(String name, T defaultValue) {
        if (mFilePath == null) mFilePath = mDefaultFilePath;
        saveValue(name, defaultValue, true);
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return null.
     *
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the double setting value.
     */
    @SuppressWarnings("unchecked")
    public Double retrieveDouble(String name) {
        try {
            if (sIsLoaded) {
                Object val = mSettings.getOrDefault(name, null);
                if (val == null) return null;
                return Double.parseDouble(val.toString());
            } else {
                log.warning("Failed to read setting as not loaded.");
                return null;
            }
        } catch (Exception e) {
            log.warning("Failed to parse as a double, maybe it isn't one?");
            return null;
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param defaultValue the value returned if failed to retrieve
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the double setting value.
     */
    public Double retrieveDouble(String name, Double defaultValue) {
        Double value = retrieveDouble(name);
        if (value == null) {
            recreateSettingsFile(name, defaultValue);
        }
        return (value != null ? value : defaultValue);
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return null.
     *
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the long setting value.
     */
    @SuppressWarnings("unchecked")
    public Long retrieveLong(String name) {
        try {
            if (sIsLoaded) {
                Object val = mSettings.getOrDefault(name, null);
                if (val == null) return null;
                return Long.parseLong(val.toString());
            } else {
                log.warning("Failed to read setting as not loaded.");
                return null;
            }
        } catch (Exception e) {
            log.warning("Failed to parse as a long, maybe it isn't one?");
            return null;
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param defaultValue the value returned if failed to retrieve
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the long setting value.
     */
    public Long retrieveLong(String name, Long defaultValue) {
        Long value = retrieveLong(name);
        if (value == null) {
            recreateSettingsFile(name, defaultValue);
        }
        return (value != null ? value : defaultValue);
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return null.
     *
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the boolean setting value.
     */
    @SuppressWarnings("unchecked")
    public Boolean retrieveBoolean(String name) {
        try {
            if (sIsLoaded) {
                Object val = mSettings.getOrDefault(name, null);
                if (val == null) return null;
                return Boolean.parseBoolean(val.toString());
            } else {
                log.warning("Failed to read setting as not loaded.");
                return null;
            }
        } catch (Exception e) {
            log.warning("Failed to parse as a bool, maybe it isn't one?");
            return null;
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param defaultValue the value returned if failed to retrieve
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the boolean setting value.
     */
    public Boolean retrieveBoolean(String name, Boolean defaultValue) {
        Boolean value = retrieveBoolean(name);
        if (value == null) {
            recreateSettingsFile(name, defaultValue);
        }
        return (value != null ? value : defaultValue);
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the integer setting value.
     */
    @SuppressWarnings("unchecked")
    public Integer retrieveInteger(String name) {
        try {
            if (sIsLoaded) {
                Object val = mSettings.getOrDefault(name, null);
                if (val == null) return null;
                return Integer.parseInt(val.toString());
            } else {
                log.warning("Failed to read setting as not loaded.");
                return -1;
            }
        } catch (Exception e) {
            log.warning("Failed to parse as an int, maybe it isn't one?");
            return -1;
        }
    }

    /**
     * Retrieves a loaded setting by name, if the settings aren't loaded or the setting doesn't
     * exist then will return default value.
     *
     * @param defaultValue the value returned if failed to retrieve
     * @param name the name of the setting as it is written in the settings.json file. Nested
     *     Settings are not allowed.
     * @return the integer setting value.
     */
    public Integer retrieveInteger(String name, Integer defaultValue) {
        Integer value = retrieveInteger(name);
        if (value == -1) {
            recreateSettingsFile(name, defaultValue);
        }
        return (value == -1 ? value : defaultValue);
    }

    public void save() {
        try {
            FileOutputStream out = new FileOutputStream(mFilePath);
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(mSettings);
                out.write(json.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.close();
        } catch (FileNotFoundException e) {
            log.severe("Cannot Find settings file");
        } catch (IOException e) {
            log.severe("failed to close stream");
        }
    }
}
