/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.dragonskulle.components.Component;

/**
 * An engine component which allows you to play audio.
 *
 * @author Dragonskulle
 *     <p>Both filename and the SoundType channel are public fields which can be accessed
 */
@Log
public class AudioSource extends Component {

    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 500;
    public String mFileName;
    public SoundType mChannel = SoundType.SFX;

    /** Constructor */
    public AudioSource() {
        super();
    }

    public AudioSource(String filename, SoundType channel) {
        super();
        mFileName = filename;
        mChannel = channel;
    }

    public void loadAudio(String filename, SoundType channel) {
        mFileName = filename;
        mChannel = channel;
    }

    public void loadAudioFromRemote(String audioUrl, SoundType channel) {
        log.info("Loading audio from remote source");
        File f = new File(Resources.getResource(UUID.randomUUID().toString()).getFile());
        log.info("created audio file destination");
        try {
            FileUtils.copyURLToFile(new URL(audioUrl), f, CONNECT_TIMEOUT, READ_TIMEOUT);
            log.info("downloaded audio file");
            mFileName = f.getName();
            mChannel = channel;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Plays the audio on the current specified channel from the current specified filename */
    public void play() {
        AudioManager.getInstance().play(mChannel, mFileName);
    }

    @Override
    protected void onDestroy() {
        // TODO: Implement onDestroy for AudioSource
    }
}
