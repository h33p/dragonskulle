/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This is a class which allows the game to play audio and to control the volume of the audio
 * @author Dragonskulle
 * <p> This is a singleton class so to use this use it as {@code AudioManager.getInstance().method()}
 */
public class AudioManager {
	
	private static final AudioManager AUDIO_MANAGER_INSTANCE = new AudioManager(); 
	private Mixer mixer;
	private DataLinePool[] sounds;
	
	public static final Logger LOGGER = Logger.getLogger("audiomanager");
	
	/**
	 * This constructor creates the AudioManager.  If the mixer is not created it is set to null.
	 */
	private AudioManager() {
		
		//Creates the mixer
		try {
			mixer = AudioSystem.getMixer(null);
			
			//Creates the different lines
			DataLinePool background = new DataLinePool(mixer, SoundType.BACKGROUND);
			DataLinePool sfx = new DataLinePool(mixer, SoundType.SFX);
			
			sounds = new DataLinePool[2];
			sounds[0] = background;
			sounds[1] = sfx;
		}
		catch (SecurityException e) {
			mixer = null;
			//TODO Log error
			
		}
		catch (IllegalArgumentException e) {
			mixer = null;
			//TODO Log error
		}
		
	}
	
	/**
	 * This will play audio from the file.  If this file does not exist then it will not be played
	 * @param channel Whether to play as Background music or as a Sound Effect
	 * @param fileName the name of the file which has the music.  Must be a .wav file
	 * @return whether the music has been successful to play
	 */
	public boolean play(SoundType channel, String fileName) { // TODO return false to denote a failure to play and true to denote a play
	
		try {
			AudioInputStream audio = AudioSystem.getAudioInputStream(new File(fileName).getAbsoluteFile());
			if (channel == SoundType.BACKGROUND) {
				sounds[0].openStream(audio);
				return true;
			}
			else {
				sounds[1].openStream(audio);
				return true;
			}
		} catch (UnsupportedAudioFileException e) {
			return false;
			//TODO Log error
		} catch (IOException e) {
			return false;
			//TODO Log error
		}
	}
	
	/**
	 * Set the mute value for a specific channel
	 * @param channel The channel to mute
	 * @param muteValue whether to mute the channel or not
	 */
	public void setMute(SoundType channel, boolean muteValue) {
		
		if (channel == SoundType.BACKGROUND) {
			sounds[0].setMute(muteValue);
		}
		else {
			sounds[1].setMute(muteValue);
		}
	}
	
	/**
	 * Get the current mute value of the selected channel 
	 * @param channel The channel to check 
	 * @return whether the channel is muted
	 */
	public boolean getMute(SoundType channel) {
		
		if (channel == SoundType.BACKGROUND) {
			return sounds[0].getMute();
		}
		else {
			return sounds[1].getMute();
		}
	}
	
	/**  
	 * Set the volume of selected channel between 0 and 100
	 * @param channel The channel to change volume
	 * @param setVol the volume to change to
	 */
	public void setVolume(SoundType channel, int setVol) {
		if (setVol > 100) {
			setVol = 100;
		}
		else if (setVol < 0) {
			setVol = 0;
		}
		
		if (channel == SoundType.BACKGROUND) {
			sounds[0].setVolume(setVol);
		}
		else {
			sounds[1].setVolume(setVol);
		}
	}
	
	/**
	 * Get the current volume of selected channel
	 * @param channel The channel to select
	 * @return The current volume
	 */
	public int getVolume(SoundType channel) {
		
		if (channel == SoundType.BACKGROUND) {
			return sounds[0].getVolume();
		}
		else {
			return sounds[1].getVolume();
		}
	}
	
	/**
	 * This closes the mixer and must be called at the end of a program.
	 */
	public void cleanup() {
		if (mixer != null) {
			mixer.close();
		}
		
	}
	
	/**
	 * This forces the class to be a singleton
	 * @return The instance of the AudioManager
	 */
	public static AudioManager getInstance() {
		return AUDIO_MANAGER_INSTANCE;
	}
	
	

}
