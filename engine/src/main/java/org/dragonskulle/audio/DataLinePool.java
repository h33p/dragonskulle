/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * This is a class which stores all the different lines used in the mixer
 * @author Dragonskulle
 *
 */
public class DataLinePool {
	
	
	private ClipClass[] sounds;
	private final int NUMBER_OF_CLIPS = 1;
	private int masterVol;
	
	public final static Logger LOGGER = Logger.getLogger("DataLine");
	
	
	/**
	 * The only Constructor to be allowed to use
	 * @param mixer The mixer to plug the clips into
	 */
	public DataLinePool(Mixer mixer, SoundType soundType) {
		
	
		sounds = new ClipClass[NUMBER_OF_CLIPS];
		
		// Creates the clips
		for (int i = 0; i < NUMBER_OF_CLIPS; i++) {
			ClipClass clip;
			
			if (soundType == SoundType.SFX) {
				try {
					clip = new ClipClass(mixer, false);
				} catch (LineUnavailableException e) {
					clip = null;
					LOGGER.log(Level.WARNING, "Clip is unabale to be made thus will not be able to play audio on this clip");
				}
			}
			else {
				try {
					clip = new ClipClass(mixer, true);
					
				} catch (LineUnavailableException e) {
					clip = null;
					LOGGER.log(Level.WARNING, "Clip is unabale to be made thus will not be able to play audio on this clip");
				}
			}
			
			sounds[i] = clip;
			masterVol = 50;
					
		}
	}

	/**
	 * Play a sound effect
	 * @param input The stream to be played
	 * @return the {@code ClipClass} which has been played on
	 */
	public ClipClass openStream(AudioInputStream input) {
		
		if (input == null) {
			return null;
		}
		
		ClipClass toUse = sounds[0];
		toUse.play(input);
		sounds[0] = toUse;
		return toUse;  //MAYBE USE REFERENCE
	}
	
	/**
	 * Set the mute value
	 * @param setMute the {@code boolean} value to set mute
	 */
	public void setMute(boolean setMute) {
		
		// Set all the Clips with new mute value
		for (int i = 0; i < NUMBER_OF_CLIPS; i++) {
			ClipClass toUse = sounds[i];
			if (toUse != null) {
				toUse.setMute(setMute);
			}
			sounds[i] = toUse;
		}
	}
	
	/**
	 * Set the volume between 0 and 100 inclusive.  If over 100 set to 100, if less than 0 set to 0
	 * @param setVol an {@code int} value between 0 and 100 inclusive
	 */
	public void setVolume(int setVol) {
		
		if (setVol > 100) {
			setVol = 100;
		}
		else if (setVol < 0) {
			setVol = 0;
		}
		// Will update all clips with the new value
		for (int i = 0; i < NUMBER_OF_CLIPS; i++) {
			ClipClass toUse = sounds[i];
			
			if (toUse != null) {
				
				toUse.setVolume(setVol);
			}
			
			sounds[i] = toUse;
		}
		masterVol = setVol;
	}
	
	/**
	 * Getter for the mute value
	 * @return mute value
	 */
	public boolean getMute() {
		
		int index = 0;
		// Find one Clip which is not null
		while (sounds[index] == null && index < NUMBER_OF_CLIPS) {
			index ++;
		}
		if (index == NUMBER_OF_CLIPS) {
			return false;
		}
		return sounds[index].getMute();
	}
	
	/**
	 * Getter for the volume value
	 * @return volume value
	 */
	public int getVolume() {
		return masterVol;
	}
	
	/**
	 * Will return an array of clips which have been used so they can removed
	 * @return An {@code array} of {@code ClipClass} 
	 */
	public ClipClass[] cleanup(){
		return sounds;
	}
}
