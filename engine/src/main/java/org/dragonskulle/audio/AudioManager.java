/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * The manager for the engine's audio system
 *
 * @author Harry Stoltz
 *     <p>This class will handle the loading and buffering of all sound files, and will also manage
 *     a pool of sources that can be used by AudioSources to play the sounds back
 */
@Accessors(prefix = "m")
@Log
public class AudioManager {
    private static final Logger LOGGER = Logger.getLogger("audio");
    private static final AudioManager AUDIO_MANAGER = new AudioManager();

    // TODO: Decide how many simultaneous audio sources we want to support
    private static final int MAX_SOURCES = 32;

    /** This constructor creates the AudioManager. If the Mixer is not created it is set to null. */
    private AudioManager() {
        initAudioManager();
    }

    /** Attempt to create MAX_SOURCES sources */
    private void setupSources() {
        for (int i = 0; i < MAX_SOURCES; i++) {
            int source = AL11.alGenSources();
            int error = AL11.alGetError();
            if (error != AL11.AL_NO_ERROR) {
                switch (error) {
                    case AL11.AL_OUT_OF_MEMORY:
                        LOGGER.warning("Error whilst creating sources (AL_OUT_OF_MEMORY)");
                        break;
                    case AL11.AL_INVALID_VALUE:
                        LOGGER.warning("Error whilst creating sources (AL_INVALID_VALUE)");
                        break;
                    case AL11.AL_INVALID_OPERATION:
                        LOGGER.warning("Error whilst creating sources (AL_INVALID_OPERATION)");
                        break;
                }
                break;
            }

            Source s = new Source();
            s.setSource(source);

            mSources.add(s);
        }
    }

    /**
     * Attempt to get the first source in mSources that is not in use. In theory, this should never
     * return null as if there are more than MAX_SOURCES AudioSources currently active, some will
     * have their source detached before this is ever called
     *
     * @return An inactive Source, or null if none were available
     */
    public boolean play(SoundType channel, String fileName) {

        try {

            String filePath = "engine/src/main/resources/audio/";

            // Creates the audio file
            AudioInputStream audio =
                    AudioSystem.getAudioInputStream(
                            new File(filePath + fileName).getAbsoluteFile());

            // Plays the file on the right channel
            if (mMixer != null && channel == SoundType.BACKGROUND) {
                mSounds[0].openStream(audio);
                return true;
            } else if (mMixer != null && channel == SoundType.SFX) {
                mSounds[1].openStream(audio);
                return true;
            } else {
                log.warning("Mixer does not exist");
                return false;
            }
        } catch (UnsupportedAudioFileException e) {
            log.warning("This is unable to be played becuase it used an unsupported Audio file");
            return false;

        } catch (IOException e) {
            log.warning(
                    "This is unable to be played becuase there is an IO exception.  Make sure the file is in the right directory");
            return false;
        }
    }

    /**
     * Detach the OpenAL source from each AudioSource in the list
     *
     * @param audioSources List of AudioSources to detach a source from
     */
    private void detachSources(List<Reference<AudioSource>> audioSources) {
        for (Reference<AudioSource> ref : audioSources) {
            ref.get().detachSource();
        }
    }

    public void initAudioManager() {
        if (mALDev != -1 || mALCtx != -1) {
            return;
        }

        if (mMixer != null && channel == SoundType.BACKGROUND) {
            mSounds[0].setMute(muteValue);
        } else if (mMixer != null && channel == SoundType.SFX) {
            mSounds[1].setMute(muteValue);
        } else {
            log.warning("Error as no mixer");
        }

        WaveSound sound = WaveSound.loadWave(file);

        if (sound == null) {
            return -1;
        }

        int id = mSounds.size();
        mSounds.add(sound);

        return id;
    }

    /**
     * Attempt to get a loaded sound by id
     *
     * @param id Integer of id that the sound was loaded with
     * @return A WaveSound object representing the sound, or null if there was no sound with that id
     */
    public WaveSound getSound(int id) {
        if (id < 0 || id >= mSounds.size()) {
            return null;
        } else {
            return mSounds.get(id);
        }
    }

    /**
     * Handles the distribution of sources between all of the AudioSources in the scene, assigning
     * sources to those that have the highest priority and removing them from those that no longer
     * need them
     */
    public void update() {
        if (mAudioListener == null) {
            return;
        }

        // First remove any references to AudioSources that are no longer valid
        mAudioSources.removeIf(ref -> !ref.isValid());

        // All references will be valid because they any invalid ones are removed at the start
        for (int i = 0; i < mAudioSources.size(); i++) {
            AudioSource audioSource = mAudioSources.get(i).get();

            // Get the distance of the source from the listener
            float distance = 100000f;
            if (mAudioListener != null && mAudioListener.isValid()) {
                distance = mAudioListener.get().getPosition().distance(audioSource.getPosition());
            }

            return mSounds[0].isMasterMute();

        } else if (mMixer != null && channel == SoundType.SFX) {
            return mSounds[1].isMasterMute();
        }

        mAudioSources.clear();
    }

    /**
     * Add an audio source so that it can have a source attached to it when required
     *
     * @param audioSource Reference to the AudioSource that will be added
     */
    public void addAudioSource(Reference<AudioSource> audioSource) {
        mAudioSources.add(audioSource);
    }

    /**
     * Get the singleton AudioListener component from the currently active scene and set it as the
     * active AudioListener
     */
    public void updateAudioListener() {
        AudioListener listener = Scene.getActiveScene().getSingleton(AudioListener.class);
        if (listener == null) {
            mAudioListener = null;
        } else {
            mAudioListener = listener.getReference(AudioListener.class);
        }
    }

    /**
     * Set the master volume for the game
     *
     * @param volume Value between 0f and 1f
     */
    public void setMasterVolume(float volume) {
        volume = Math.max(0f, volume);
        volume = Math.min(volume, 1f);

        if (mMixer != null && channel == SoundType.BACKGROUND) {
            return mSounds[0].getMasterVol();

        } else if (mMixer != null && channel == SoundType.SFX) {
            return mSounds[1].getMasterVol();
        }
    }

    public void toggleMasterMute() {
        setMasterMute(!mMasterMuted);
    }

    /** Cleanup all resources still in use */
    public void cleanup() {
        if (mALDev == -1) {
            return;
        }

        for (Source s : mSources) {
            AL11.alDeleteSources(s.getSource());
        }
        mSources.clear();

        for (WaveSound s : mSounds) {
            AL11.alDeleteBuffers(s.buffer);
        }
        mSounds.clear();

        ALC11.alcMakeContextCurrent(0L);
        ALC11.alcDestroyContext(mALCtx);
        ALC11.alcCloseDevice(mALDev);
        mALDev = -1;
        mALCtx = -1;
    }

    /**
     * Get the singleton instance of the audio manager
     *
     * @return AudioManager instance
     */
    public static AudioManager getInstance() {
        return AUDIO_MANAGER;
    }
}
