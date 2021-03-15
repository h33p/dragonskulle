package org.dragonskulle.game.audio;

import org.dragonskulle.audio.AudioSource;
import org.dragonskulle.audio.SoundType;
import org.dragonskulle.ui.UIButton.IButtonEvent;

/**
 * A class which allows you to add sound to button clicks
 * @author Dragonskulle
 *
 */
public class GameAudio {
	
	private AudioSource mAudio;
	
	/**
	 * The constructor 
	 * @param filename The file which is in resources/audio
	 * @param channel The channel to play on
	 */
	public GameAudio(String filename, SoundType channel) {
		mAudio = new AudioSource();
		mAudio.filename = filename;
		mAudio.channel = channel;
	}
	
	/**
	 * The button click to play the sound
	 * @param onClick The stuff to do when clicking the button
	 * @return The stuff to do when clicking a button + playing a sound
	 */
	public IButtonEvent audibleClick(IButtonEvent onClick) {
		return (a, b) -> {
			mAudio.play();
			if (onClick != null) {
				onClick.eventHandler(a, b);
			}
		};
	}
}