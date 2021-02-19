package org.dragonskulle.audio;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;


public class AudioManagerTest {

	
	// TO EVER USE AUDIOMANAGER DO
	/* AudioManager.getInstance().method()*/
	
	@Test
	public void createTest() {
		AudioManager audioManager = AudioManager.getInstance();
		Assert.assertNotNull(audioManager);
		
	}
	
	@Test
	public void muteBackgroundTest() {
		AudioManager.getInstance().play(SoundType.BACKGROUND, "waves.wav", null);
		
		Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setMute(SoundType.BACKGROUND, true);
		Assert.assertTrue(AudioManager.getInstance().getMute(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setMute(SoundType.BACKGROUND, false);
		Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.BACKGROUND));
	}
	
	@Test
	public void muteSFXTest() {
		AudioManager.getInstance().play(SoundType.SFX, "thunderclap.wav", null);
		
		Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.SFX));
		
		AudioManager.getInstance().setMute(SoundType.SFX, true);
		Assert.assertTrue(AudioManager.getInstance().getMute(SoundType.SFX));
		
		AudioManager.getInstance().setMute(SoundType.SFX, false);
		Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.SFX));
	}
	
	@Test
	public void volumeBackgroundTest() {
	
		AudioManager.getInstance().play(SoundType.BACKGROUND, "waves.wav", null);
		
		Assert.assertEquals(50, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 30);
		Assert.assertEquals(30, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 0);
		Assert.assertEquals(0, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 87);
		Assert.assertEquals(87, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));

		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 100);
		Assert.assertEquals(100, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, -1);
		Assert.assertEquals(0, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 100);
		Assert.assertEquals(100, AudioManager.getInstance().getVolume(SoundType.BACKGROUND));
		
	}
	
	@Test
	public void volumeSFXTest() {
	
		AudioManager.getInstance().play(SoundType.SFX, "thunderclap.wav", null);
		
		Assert.assertEquals(50, AudioManager.getInstance().getVolume(SoundType.SFX));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 30);
		Assert.assertEquals(30, AudioManager.getInstance().getVolume(SoundType.SFX));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 0);
		Assert.assertEquals(0, AudioManager.getInstance().getVolume(SoundType.SFX));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 87);
		Assert.assertEquals(87, AudioManager.getInstance().getVolume(SoundType.SFX));

		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 100);
		Assert.assertEquals(100, AudioManager.getInstance().getVolume(SoundType.SFX));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, -1);
		Assert.assertEquals(0, AudioManager.getInstance().getVolume(SoundType.SFX));
		
		AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 100);
		Assert.assertEquals(100, AudioManager.getInstance().getVolume(SoundType.SFX));
		
	}
	
	@After
	public void cleanUp() {
		AudioManager.getInstance().cleanup();
	}
	
}
