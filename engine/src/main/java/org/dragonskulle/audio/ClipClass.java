package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This will hold all the information needed for each Clip
 * @author Dragonskulle
 *
 */
public class ClipClass {
	
	private Clip clip;
	private BooleanControl mute;
	private FloatControl volume;
	private int currentVol;
	private boolean looping;
	
	public static final Logger LOGGER = Logger.getLogger("audio");
	
	
	/**
	 * The Constructor which creates the class
	 * @param mixer The mixer to be plug the clip into
	 * @param loopContinuously whether the clip needs to loop continuously
	 * @throws LineUnavailableException If the clip cannot be added
	 */
	public ClipClass(Mixer mixer, boolean loopContinuously) throws LineUnavailableException {
		
		// Gets the line
		DataLine.Info dataLine = new DataLine.Info(Clip.class, null);
		clip = (Clip) mixer.getLine(dataLine);
		
		// Tries to open the audio stream
		try {
			AudioInputStream startingStream = AudioSystem.getAudioInputStream(new File("Silent.wav").getAbsoluteFile());
			clip.open(startingStream);
			
		} catch (UnsupportedAudioFileException e) {
			LOGGER.log(Level.WARNING, "Unable to open Silent.wav.  Please tell someone sooner rather than later");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to open Silent.wav.  Please tell someone sooner rather than later");
		} 
		
		
		// Gets mute and volume control and sets them
		mute = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
		mute.setValue(false);
		
		volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		
		currentVol = 0;
		
		setVolume(50);  /* The default volume */
		
		// Loops the clip if needs to 
		if (loopContinuously) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		looping = loopContinuously;
			
	}
	
	/**
	 * Setter of the mute value
	 * @param muteValue Whether to mute or not
	 */
	public void setMute(boolean muteValue) {
		mute.setValue(muteValue);
	}
	
	/**
	 * Getter of mute value
	 * @return the current mute value
	 */
	public boolean getMute() {
		return mute.getValue();
	}
	
	/**
	 * Set the volume value on the clip
	 * @param newVolume the new volume to use
	 */
	public void setVolume(int newVolume) {
		
		// Sets the value between 0 and 100
		if (newVolume < 0) {
			newVolume = 0;
		}
		else if (newVolume > 100) {
			newVolume = 100;
		}
		
		
		currentVol = newVolume;
		
		// Gets the range of volume possible for this clip
		float amountOfVolForClip = Math.abs(volume.getMaximum()) + Math.abs(volume.getMinimum());
		
		// Sets the new volume 
		float newVol = (((float) newVolume / 100) * amountOfVolForClip) + volume.getMinimum();
		
		// Set this volume
		volume.setValue(newVol);
	}
	
	/**
	 * Gets the current volume
	 * @return the current volume
	 */
	public int getVolume() {
		return currentVol;
	}
	
	/**
	 * Whether the clip is looping
	 * @return
	 */
	public boolean getLooping() {
		return looping;
	}
	
	/**
	 * Plays the audio
	 * @param audio the audio stream to play
	 * @return The clip just used
	 */
	public Clip play(AudioInputStream audio) {
		
		clip.close();
		try {
			clip.open(audio);
		} catch (LineUnavailableException e) {
			LOGGER.log(Level.WARNING, "The line is unavailable");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "This file does not exist");
		}
		clip.setMicrosecondPosition(0);
		clip.start();
		
		if (clip.isActive() && looping) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		return clip;		
	}
	

}
