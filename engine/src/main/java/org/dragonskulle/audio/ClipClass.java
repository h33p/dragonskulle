package org.dragonskulle.audio;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * This will hold all the information needed for Clips
 * @author low101043
 *
 */
public class ClipClass {
	
	private Clip clip;
	private BooleanControl mute;
	private FloatControl volume;
	private int currentVol;
	private boolean looping;
	
	
	public ClipClass(Mixer mixer, boolean loopContinuously) throws LineUnavailableException {
		
		DataLine.Info dataLine = new DataLine.Info(Clip.class, null);
		clip = (Clip) mixer.getLine(dataLine);
		
		mute = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
		mute.setValue(false);
		
		volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		
		currentVol = 0;
		
		setVolume(50);
		
		if (loopContinuously) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		looping = loopContinuously;
		
		clip.open();
	}
	
	public void setMute(boolean muteValue) {
		mute.setValue(muteValue);
	}
	
	public boolean getMute() {
		return mute.getValue();
	}
	
	public void setVolume(int newVolume) {
		
		if (newVolume < 0) {
			newVolume = 0;
		}
		else if (newVolume > 100) {
			newVolume = 100;
		}
		
		currentVol = newVolume;
		
		float amountOfVolForClip = Math.abs(volume.getMaximum()) + Math.abs(volume.getMinimum());
		
		float newVol = (((float) newVolume / 100) * amountOfVolForClip) + volume.getMinimum();
		
		volume.setValue(newVol);
	}
	
	public int getVolume() {
		return currentVol;
	}
	
	public boolean getLooping() {
		return looping;
	}
	
	public Clip play(AudioInputStream audio) {
		clip.close();
		try {
			clip.open(audio);
		} catch (LineUnavailableException e) {
			//TODO Log
		} catch (IOException e) {
			//TODO Log
		}
		clip.setMicrosecondPosition(0);
		clip.start();
		
		if (clip.isActive() && looping) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		return clip;
		
		
	}
	

}
