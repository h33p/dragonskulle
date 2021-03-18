/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import org.dragonskulle.components.Component;

/**
 * An engine component which allows you to play audio.
 *
 * @author Dragonskulle
 * <p>Both filename and the SoundType channel are public fields which can be accessed
 */
public class AudioSource extends Component {

    public String mFileName;
    public SoundType mChannel = SoundType.SFX;

    /**
     * Constructor
     */
    public AudioSource() {
        super();
    }

    public AudioSource(String filename, SoundType channel) {
        super();
        mFileName = filename;
        mChannel = channel;
    }

    /**
     * Plays the audio on the current specified channel from the current specified filename
     */
    public void play() {
        AudioManager.getInstance().play(mChannel, mFileName);
    }

    @Override
    protected void onDestroy() {
        // TODO: Implement onDestroy for AudioSource
    }
}
