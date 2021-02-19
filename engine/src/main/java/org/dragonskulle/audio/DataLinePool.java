/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

/**
 * Class which stores the different clips to play sound effects 
 * @author Dragonskulle
 *
 */
public class DataLinePool {

	/**
	 * Play a sound effect
	 * @param input The stream to be played
	 * @return the {@code Clip} which has been played on
	 */
	public Clip openStream(AudioInputStream input) {
		return null;
	}
	
	/**
	 * Set the mute value
	 * @param setMute the {@code boolean} value to set mute
	 */
	public void setMute(boolean setMute) {
		;
	}
	
	/**
	 * Set the volume
	 * @param setVol an {@code int} value between 0 and 100 inclusive
	 */
	public void setVolume(int setVol) {
		;
	}
	
	/**
	 * Will return an array of clips which have been used so they can removed
	 * @return An {@code array} of {@code Clips} 
	 */
	public Clip[] cleanup(){
		return null;
	}
}
