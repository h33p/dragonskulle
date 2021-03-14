package org.dragonskulle.game.audio;

import org.dragonskulle.audio.AudioSource;
import org.dragonskulle.ui.UIButton.IButtonEvent;

public class GameAudio {
	
	AudioSource audio;
	public GameAudio() {
		audio = new AudioSource();
		audio.filename = "button-10.wav";
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