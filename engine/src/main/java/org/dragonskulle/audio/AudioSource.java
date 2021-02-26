/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import org.dragonskulle.components.Component;

/**
 * An engine component which allows you to play audio.
 *
 * @author Dragonskulle
 *     <p>Both filename and the SoundType channel are public fields which can be accessed </p>
 */
public class AudioSource extends Component {

    public String filename;
    public SoundType channel = SoundType.SFX;

    /** Constructor */
    public AudioSource() {
        ;
    }

    /** Plays the audio on the current specified channel from the current specified filename */
    public void play() {
        AudioManager.getInstance().play(channel, filename);
    }
}
