/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

/**
 * Class which stores the different clips to play sound effects 
 * @author Dragonskulle
 *
 */
public class DataLinePool {
	
	/**
	 * The only Constructor to be allowed to use
	 * @param mixer The mixer to plug the clips into
	 */
	public DataLinePool(Mixer mixer) {
		;
	}

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
	 * Set the volume between 0 and 100 inclusive.  If over 100 set to 100, if less than 0 set to 0
	 * @param setVol an {@code int} value between 0 and 100 inclusive
	 */
	public void setVolume(int setVol) {
		;
	}
	
	/**
	 * Getter
	 * @return mute value
	 */
	public boolean getMute() {
		return (Boolean) null;
	}
	
	/**
	 * Getter
	 * @return volume value
	 */
	public int getVolume() {
		return -1;
	}
	
	/**
	 * Will return an array of clips which have been used so they can removed
	 * @return An {@code array} of {@code Clips} 
	 */
	public Clip[] cleanup(){
		return null;
	}
}
