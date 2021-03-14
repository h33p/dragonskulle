package org.dragonskulle.game.audio;

import org.dragonskulle.audio.AudioSource;
import org.dragonskulle.audio.SoundType;
import org.dragonskulle.ui.UIButton.IButtonEvent;

public class GameAudio {
	
	AudioSource audio;
	public GameAudio(String filename, SoundType channel) {
		audio = new AudioSource();
		audio.filename = filename;
		audio.channel = channel;
	}
	
	public IButtonEvent audibleClick(IButtonEvent onClick) {
		return (a, b) -> {
			audio.play();
			if (onClick != null) {
				onClick.eventHandler(a, b);
			}
		};
	}
}