package org.dragonskulle.game;

/**
 * @author Oscar L
 * For loading in any settings from the settings.conf file.
 */
public class Settings {
    private static final Settings instance = new Settings();

    private Settings() {
    }

    void loadSettings(){

    }

    public static Settings getInstance() {
        return instance;
    }
}