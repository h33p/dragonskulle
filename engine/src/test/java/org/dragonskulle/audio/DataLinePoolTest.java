/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.Assert;
import org.junit.Test;

public class DataLinePoolTest {

    @Test
    public void createDataLineTest() {

        // Checks whether a DataLinePool object can be created
        Mixer mixer = AudioSystem.getMixer(null);

        DataLinePool dataLine = new DataLinePool(mixer, SoundType.BACKGROUND);

        Assert.assertNotNull(dataLine);
    }

    @Test
    public void openStreamTest() throws UnsupportedAudioFileException, IOException {
        // Not fully complete because no way for machine to check whether sound actually on

        Mixer mixer = AudioSystem.getMixer(null);

        DataLinePool dataLine = new DataLinePool(mixer, SoundType.BACKGROUND);

        Assert.assertNotNull(dataLine);

        AudioInputStream audio =
                AudioSystem.getAudioInputStream(new File("waves.wav").getAbsoluteFile());

        ClipClass clip = dataLine.openStream(audio);

        Assert.assertNotNull(clip);

        clip = dataLine.openStream(null);

        Assert.assertNull(clip);
    }

    @Test
    public void muteTest() throws UnsupportedAudioFileException, IOException {
        // Not complete as no way for machine to check whether sound really muted

        Mixer mixer = AudioSystem.getMixer(null);

        DataLinePool dataLine = new DataLinePool(mixer, SoundType.BACKGROUND);

        Assert.assertNotNull(dataLine);

        AudioInputStream audio =
                AudioSystem.getAudioInputStream(new File("waves.wav").getAbsoluteFile());
        ;

        ClipClass clip = dataLine.openStream(audio);

        Assert.assertFalse(dataLine.getMute()); // checks when created it is started as mute

        dataLine.setMute(true);

        Assert.assertTrue(dataLine.getMute()); // checks it can change value

        dataLine.setMute(true);
        Assert.assertTrue(dataLine.getMute());

        dataLine.setMute(false);
        Assert.assertFalse(dataLine.getMute());
    }

    @Test
    public void volumeTest()
            throws UnsupportedAudioFileException, IOException, LineUnavailableException {

        // Not complete as no way for machine to check whether sound really has changed
        Mixer mixer = AudioSystem.getMixer(null);

        DataLinePool dataLine = new DataLinePool(mixer, SoundType.SFX);

        Assert.assertNotNull(dataLine);

        AudioInputStream audio =
                AudioSystem.getAudioInputStream(new File("waves.wav").getAbsoluteFile());
        ;

        ClipClass clip = dataLine.openStream(audio);

        Assert.assertEquals(50, dataLine.getVolume());

        Assert.assertNotNull(dataLine);

        dataLine.setVolume(60);

        Assert.assertEquals(60, dataLine.getVolume());

        dataLine.setVolume(100);

        Assert.assertEquals(100, dataLine.getVolume());

        dataLine.setVolume(0);
        Assert.assertEquals(0, dataLine.getVolume());

        dataLine.setVolume(48);
        Assert.assertEquals(48, dataLine.getVolume());

        dataLine.setVolume(-14);
        Assert.assertEquals(0, dataLine.getVolume());

        dataLine.setVolume(106);
        Assert.assertEquals(100, dataLine.getVolume());
    }

    @Test
    public void cleanupTest() throws UnsupportedAudioFileException, IOException {

        Mixer mixer = AudioSystem.getMixer(null);

        DataLinePool dataLine = new DataLinePool(mixer, SoundType.SFX);

        Assert.assertNotNull(dataLine);

        AudioInputStream audio =
                AudioSystem.getAudioInputStream(new File("waves.wav").getAbsoluteFile());
        ;

        ClipClass clip = dataLine.openStream(audio);

        AudioInputStream audio2 =
                AudioSystem.getAudioInputStream(new File("thunderclap.wav").getAbsoluteFile());
        ;

        ClipClass clip2 = dataLine.openStream(audio2);

        ClipClass[] clips = dataLine.cleanup();

        Assert.assertSame(clip2, clips[0]);
    }
}
