/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This will hold all the information needed for each Clip
 *
 * @author Dragonskulle
 */
public class AudioClip {

    private Clip mClip;
    private BooleanControl mMute;
    private FloatControl mVolume;
    private int mCurrentVol;
    private boolean mLooping;

    public static final Logger LOGGER = Logger.getLogger("audio");

    /**
     * The Constructor which creates the class
     *
     * @param mixer The mixer to be plug the clip into
     * @param loopContinuously whether the clip needs to loop continuously
     * @throws LineUnavailableException If the clip cannot be added
     */
    public AudioClip(Mixer mixer, boolean loopContinuously) throws LineUnavailableException {

        // Gets the line
        DataLine.Info dataLine = new DataLine.Info(Clip.class, null);
        mClip = (Clip) mixer.getLine(dataLine);

        // Tries to open the audio stream.  Should not really get to the catch statements as
        // Silent.wav should be somewhere.
        try {
            AudioInputStream startingStream =
                    AudioSystem.getAudioInputStream(new File("Silent.wav").getAbsoluteFile());
            mClip.open(startingStream);

        } catch (UnsupportedAudioFileException | IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Unable to open Silent.wav.  Please tell someone sooner rather than later");
        }
        // Gets mute and mVolume control and sets them
        mMute = (BooleanControl) mClip.getControl(BooleanControl.Type.MUTE);
        mMute.setValue(false);

        mVolume = (FloatControl) mClip.getControl(FloatControl.Type.MASTER_GAIN);

        mCurrentVol = 0;

        setVolume(50); /* The default mVolume */

        // Loops the clip if needs to
        if (loopContinuously) {
            mClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        mLooping = loopContinuously;
    }

    /**
     * Setter of the mute value
     *
     * @param mMuteValue Whether to mute or not
     */
    public void setMute(boolean mMuteValue) {
        mMute.setValue(mMuteValue);
    }

    /**
     * Getter of mute value
     *
     * @return the current mute value
     */
    public boolean getMute() {
        return mMute.getValue();
    }

    /**
     * Set the Volume value on the mClip
     *
     * @param newVolume the new Volume to use
     */
    public void setVolume(int newVolume) {

        // Sets the value between 0 and 100
        if (newVolume < 0) {
            newVolume = 0;
        } else if (newVolume > 100) {
            newVolume = 100;
        }

        mCurrentVol = newVolume;

        // Gets the range of mVolume possible for this Clip
        float amountOfVolForClip = Math.abs(mVolume.getMaximum()) + Math.abs(mVolume.getMinimum());

        // Sets the new Volume
        float newVol = (((float) newVolume / 100) * amountOfVolForClip) + mVolume.getMinimum();

        // Set this Volume
        mVolume.setValue(newVol);
    }

    /**
     * Gets the current Volume
     *
     * @return the current Volume
     */
    public int getVolume() {
        return mCurrentVol;
    }

    /**
     * Whether the Clip is Looping
     *
     * @return
     */
    public boolean getLooping() {
        return mLooping;
    }

    /**
     * Plays the audio
     *
     * @param audio the audio stream to play
     * @return The Clip just used
     */
    public Clip play(AudioInputStream audio) {

        mClip.close();
        try {
            mClip.open(audio);
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.WARNING, "The line is unavailable");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "This file does not exist");
        }
        mClip.setMicrosecondPosition(0);
        mClip.start();

        if (mClip.isActive() && mLooping) {
            mClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        return mClip;
    }
}
