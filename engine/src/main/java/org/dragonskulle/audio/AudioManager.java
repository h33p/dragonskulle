/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * This is a class which allows the game to play audio and to control the volume of the audio
 *
 * @author Dragonskulle
 *     <p>This is a singleton class so to use this use it as {@code
 *     AudioManager.getInstance().method()}
 */
@Accessors(prefix = "m")
@Log
public class AudioManager {

    private static final AudioManager AUDIO_MANAGER_INSTANCE = new AudioManager();
    private Mixer mMixer;
    private DataLinePool[] mSounds;

    /** This constructor creates the AudioManager. If the Mixer is not created it is set to null. */
    private AudioManager() {

        // Creates the mixer
        try {
            mMixer = AudioSystem.getMixer(null);

            // Creates the different lines
            DataLinePool background = new DataLinePool(mMixer, SoundType.BACKGROUND);
            DataLinePool sfx = new DataLinePool(mMixer, SoundType.SFX);

            mSounds = new DataLinePool[2];
            mSounds[0] = background;
            mSounds[1] = sfx;
        } catch (SecurityException | IllegalArgumentException e) {
            mMixer = null;
            mSounds = null;
        }
    }

    /**
     * This will play audio from the file. If this file does not exist then it will not be played
     *
     * @param channel Whether to play as Background music or as a Sound Effect
     * @param fileName the name of the file which has the music. Must be a .wav file
     * @return whether the music has been successful to play
     */
    public boolean play(SoundType channel, String fileName) {

        try {

            String filePath = "engine/src/main/resources/audio/";

            // Creates the audio file
            AudioInputStream audio =
                    AudioSystem.getAudioInputStream(
                            new File(filePath + fileName).getAbsoluteFile());

            // Plays the file on the right channel
            if (mMixer != null && channel == SoundType.BACKGROUND) {
                mSounds[0].openStream(audio);
                return true;
            } else if (mMixer != null && channel == SoundType.SFX) {
                mSounds[1].openStream(audio);
                return true;
            } else {
                log.warning("Mixer does not exist");
                return false;
            }
        } catch (UnsupportedAudioFileException e) {
            log.warning("This is unable to be played becuase it used an unsupported Audio file");
            return false;

        } catch (IOException e) {
            log.warning(
                    "This is unable to be played becuase there is an IO exception.  Make sure the file is in the right directory");
            return false;
        }
    }

    /**
     * Set the mute value for a specific channel
     *
     * @param channel The channel to mute
     * @param muteValue whether to mute the channel or not
     */
    public void setMute(SoundType channel, boolean muteValue) {

        // Sets the mute value

        if (mMixer != null && channel == SoundType.BACKGROUND) {
            mSounds[0].setMute(muteValue);
        } else if (mMixer != null && channel == SoundType.SFX) {
            mSounds[1].setMute(muteValue);
        } else {
            log.warning("Error as no mixer");
        }
    }

    /**
     * Get the current mute value of the selected channel
     *
     * @param channel The channel to check
     * @return whether the channel is muted
     */
    public boolean getMute(SoundType channel) {

        // Gets the mute value
        if (mMixer != null && channel == SoundType.BACKGROUND) {

            return mSounds[0].isMasterMute();

        } else if (mMixer != null && channel == SoundType.SFX) {
            return mSounds[1].isMasterMute();
        }
        return false; // TODO any better way?
    }

    /**
     * Set the volume of selected channel between 0 and 100
     *
     * @param channel The channel to change volume
     * @param setVol the volume to change to
     */
    public void setVolume(SoundType channel, int setVol) {

        // Changes the volume
        if (mMixer != null && channel == SoundType.BACKGROUND) {
            mSounds[0].setVolume(setVol);
        } else if (mMixer != null && channel == SoundType.SFX) {
            mSounds[1].setVolume(setVol);
        }
    }

    /**
     * Get the current volume of selected channel
     *
     * @param channel The channel to select
     * @return The current volume - Returns -1 if failure on that channel
     */
    public int getVolume(SoundType channel) {

        if (mMixer != null && channel == SoundType.BACKGROUND) {
            return mSounds[0].getMasterVol();

        } else if (mMixer != null && channel == SoundType.SFX) {
            return mSounds[1].getMasterVol();
        }

        return -1;
    }

    /** This closes the Mixer and must be called at the end of a program. */
    public void cleanup() {

        if (mMixer != null) {
            mMixer.close();
        }
    }

    /**
     * This forces the class to be a singleton
     *
     * @return The instance of the AudioManager
     */
    public static AudioManager getInstance() {
        return AUDIO_MANAGER_INSTANCE;
    }

    public void toggleMute(SoundType background) {
        this.setMute(background, !this.getMute(background));
    }
}
