/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

/**
 * Class which plays background music and sound effects.  WIll be singleton
 * @author Dragonskulle
 *
 */
public class AudioManager {
	
	//TODO Make instance - Look at Engine
	private AudioManager() {
		;
	}
	
	/**
	 * Will play some audio
	 * @param channel Whether to play as Background music or as a Sound Effect
	 * @param fileName the name of the file which has the music.  Must be a .wav file
	 * @param distance How far from the source the camera is.  Use null to ignore
	 */
	public void play(SoundType channel, String fileName, Integer distance)
	{
		;
	}
	
	/**
	 * Set the mute value for a specific channel
	 * @param channel The channel to mute
	 * @param muteValue whether to mute the channel or not
	 */
	public void setMute(SoundType channel, boolean muteValue) {
		;
	}
	
	/**
	 * Get the current mute value of the selected channel 
	 * @param channel The channel to check 
	 * @return whether the channel is muted
	 */
	public boolean getMute(SoundType channel) {
		return true;
	}
	
	/**  
	 * Set the volume of selected channel between 0 and 100
	 * @param channel The channel to change volume
	 * @param setVol the volume to change to
	 */
	public void setVolume(SoundType channel, int setVol) {
		;
	}
	
	/**
	 * Get the current volume of selected channel
	 * @param channel The channel to select
	 * @return The current volume
	 */
	public int getVolume(SoundType channel) {
		return -1;
	}
	
	/**
	 * Destroys all Clips.   Must be called at end of program
	 */
	public void cleanup() {
		;
	}
	
	static AudioManager getInstance() {
		return null;
	}
	
	

}
