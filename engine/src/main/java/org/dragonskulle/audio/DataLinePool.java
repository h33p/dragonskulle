/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * This is a class which stores all the different lines used in the mixer
 *
 * @author Dragonskulle
 */
public class DataLinePool {

    private AudioClip[] mSounds;
    private final int NUMBER_OF_CLIPS = 1;
    private int mMasterVol;
    private boolean mMasterMute;

    public static final Logger LOGGER = Logger.getLogger("DataLine");

    /**
     * The only Constructor to be allowed to use
     *
     * @param mixer The mixer to plug the clips into
     */
    public DataLinePool(Mixer mixer, SoundType soundType) {

        mSounds = new AudioClip[NUMBER_OF_CLIPS];

        // Creates the clips
        for (int i = 0; i < NUMBER_OF_CLIPS; i++) {
            AudioClip clip;

            if (soundType == SoundType.SFX) {
                try {
                    clip = new AudioClip(mixer, false);
                } catch (LineUnavailableException e) {
                    clip = null;
                    LOGGER.log(
                            Level.WARNING,
                            "Clip is unabale to be made thus will not be able to play audio on this clip");
                }
            } else {
                try {
                    clip = new AudioClip(mixer, true);

                } catch (LineUnavailableException e) {
                    clip = null;
                    LOGGER.log(
                            Level.WARNING,
                            "Clip is unabale to be made thus will not be able to play audio on this clip");
                }
            }

            mSounds[i] = clip;
            mMasterVol = 50;
        }
    }

    /**
     * Play a sound effect
     *
     * @param input The stream to be played
     * @return the {@code ClipClass} which has been played on
     */
    public AudioClip openStream(AudioInputStream input) {

        if (input == null) {
            return null;
        }

        AudioClip toUse = mSounds[0];
        toUse.play(input);
        mSounds[0] = toUse;
        return toUse; // MAYBE USE REFERENCE
    }

    /**
     * Set the mute value
     *
     * @param setMute the {@code boolean} value to set mute
     */
    public void setMute(boolean setMute) {

        mMasterMute = setMute;
        // Set all the Clips with new mute value
        for (int i = 0; i < NUMBER_OF_CLIPS; i++) {
            AudioClip toUse = mSounds[i];
            if (toUse != null) {
                toUse.setMute(mMasterMute);
            }
            mSounds[i] = toUse;
        }
    }

    /**
     * Set the volume between 0 and 100 inclusive. If over 100 set to 100, if less than 0 set to 0
     *
     * @param setVol an {@code int} value between 0 and 100 inclusive
     */
    public void setVolume(int setVol) {

        if (setVol > 100) {
            setVol = 100;
        } else if (setVol < 0) {
            setVol = 0;
        }
        // Will update all clips with the new value
        for (int i = 0; i < NUMBER_OF_CLIPS; i++) {
            AudioClip toUse = mSounds[i];

            if (toUse != null) {

                toUse.setVolume(setVol);
            }

            mSounds[i] = toUse;
        }
        mMasterVol = setVol;
    }

    /**
     * Getter for the mute value
     *
     * @return mute value
     */
    public boolean getMute() {

        return mMasterMute;
    }

    /**
     * Getter for the volume value
     *
     * @return volume value
     */
    public int getVolume() {
        return mMasterVol;
    }

    /**
     * Will return an array of clips which have been used so they can removed
     *
     * @return An {@code array} of {@code ClipClass}
     */
    public AudioClip[] cleanup() {
        return mSounds;
    }
}
