/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import org.junit.Assert;
import org.junit.Test;

public class AudioManagerTest {

    // TO EVER USE AUDIOMANAGER DO
    /* AudioManager.getInstance().method()*/

    @Test
    public void createTest() {

        Assert.assertNotNull(AudioManager.getInstance());
    }

    @Test
    public void playTest() {

        Assert.assertFalse(
                AudioManager.getInstance().play(SoundType.BACKGROUND, "doesntExist.wav"));
        Assert.assertFalse(AudioManager.getInstance().play(SoundType.SFX, "doesntExist.wav"));

        Assert.assertFalse(
                AudioManager.getInstance().play(SoundType.BACKGROUND, "doesntExist.txt"));
        Assert.assertFalse(AudioManager.getInstance().play(SoundType.SFX, "doesntExist.txt"));

        Assert.assertFalse(AudioManager.getInstance().play(SoundType.BACKGROUND, "pom.xml"));
        Assert.assertFalse(AudioManager.getInstance().play(SoundType.SFX, "pom.xml"));
    }

    @Test
    public void muteBackgroundTest() {
        if (AudioManager.getInstance().play(SoundType.BACKGROUND, "waves.wav")) {

            Assert.assertTrue(AudioManager.getInstance().play(SoundType.BACKGROUND, "waves.wav"));

            Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.BACKGROUND));

            AudioManager.getInstance().setMute(SoundType.BACKGROUND, true);
            Assert.assertTrue(AudioManager.getInstance().getMute(SoundType.BACKGROUND));

            AudioManager.getInstance().setMute(SoundType.BACKGROUND, false);
            Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.BACKGROUND));
        } else {
            // Most like cos Mixer does not exist
        }
    }

    @Test
    public void muteSFXTest() {

        Assert.assertNotNull(AudioManager.getInstance());
        if (AudioManager.getInstance().play(SoundType.SFX, "thunderclap.wav") == true) {
            Assert.assertTrue(AudioManager.getInstance().play(SoundType.SFX, "thunderclap.wav"));

            Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.SFX));

            AudioManager.getInstance().setMute(SoundType.SFX, true);
            Assert.assertTrue(AudioManager.getInstance().getMute(SoundType.SFX));

            AudioManager.getInstance().setMute(SoundType.SFX, false);
            Assert.assertFalse(AudioManager.getInstance().getMute(SoundType.SFX));
        } else {
            // High Chance its cos a mixer does not exist.  To test this: ?? (Ask)

        }
    }

    @Test
    public void volumeBackgroundTest() {

        if (AudioManager.getInstance().play(SoundType.BACKGROUND, "waves.wav")) {

            Assert.assertTrue(AudioManager.getInstance().play(SoundType.BACKGROUND, "waves.wav"));

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
        } else {
            // Most likely cos Mixer does not exist
        }
    }

    @Test
    public void volumeSFXTest() {

        if (AudioManager.getInstance().play(SoundType.SFX, "thunderclap.wav")) {

            Assert.assertTrue(AudioManager.getInstance().play(SoundType.SFX, "thunderclap.wav"));

            Assert.assertEquals(50, AudioManager.getInstance().getVolume(SoundType.SFX));

            AudioManager.getInstance().setVolume(SoundType.SFX, 30);
            Assert.assertEquals(30, AudioManager.getInstance().getVolume(SoundType.SFX));

            AudioManager.getInstance().setVolume(SoundType.SFX, 0);
            Assert.assertEquals(0, AudioManager.getInstance().getVolume(SoundType.SFX));

            AudioManager.getInstance().setVolume(SoundType.SFX, 87);
            Assert.assertEquals(87, AudioManager.getInstance().getVolume(SoundType.SFX));

            AudioManager.getInstance().setVolume(SoundType.SFX, 100);
            Assert.assertEquals(100, AudioManager.getInstance().getVolume(SoundType.SFX));

            AudioManager.getInstance().setVolume(SoundType.SFX, -1);
            Assert.assertEquals(0, AudioManager.getInstance().getVolume(SoundType.SFX));

            AudioManager.getInstance().setVolume(SoundType.SFX, 100);
            Assert.assertEquals(100, AudioManager.getInstance().getVolume(SoundType.SFX));
        } else {
            // Most likely cos Mixer does not exist
        }
    }
}
