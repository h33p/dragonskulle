/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

/**
 * The manager for the engine's audio system
 *
 * @author Harry Stoltz
 *     <p>This class will handle the loading and buffering of all sound files, and will also manage
 *     a pool of sources that can be used by AudioSources to play the sounds back
 */
@Accessors(prefix = "m")
public class AudioManager {
    private static final Logger LOGGER = Logger.getLogger("audio");
    private static final AudioManager AUDIO_MANAGER = new AudioManager();

    // TODO: Decide how many simultaneous audio sources we want to support
    private static final int MAX_SOURCES = 32;

    private final ArrayList<WaveSound> mSounds = new ArrayList<>();
    private final ArrayList<Source> mSources = new ArrayList<>();
    private final ArrayList<Reference<AudioSource>> mAudioSources = new ArrayList<>();

    @Getter private Reference<AudioListener> mAudioListener;
    private long mALDev = -1;
    private long mALCtx = -1;

    @Getter private float mMasterVolume;

    @Getter private boolean mInitialized = false;

    /**
     * Constructor for AudioManager. It's private as AudioManager is designed as a singleton. Opens
     * an OpenAL device and creates and sets the context for the process. Also attempts to create up
     * to MAX_SOURCES sources
     */
    private AudioManager() {
        // TODO: For now I'm just using the "best" device available, I have made AudioDevices.java
        //       which can be used to enumerate devices so we can allow user to choose in the future
        long device = ALC11.alcOpenDevice((ByteBuffer) null);
        if (device == 0L) {
            LOGGER.severe("Failed to open default OpenAL device, no audio will be available");
            return;
        }

        long ctx = ALC11.alcCreateContext(device, (IntBuffer) null);
        if (!ALC11.alcMakeContextCurrent(ctx)) {
            LOGGER.severe("Failed to set OpenAL context, no audio will be available");
            ALC11.alcCloseDevice(device);
            return;
        }

        // Get and set OpenAL capabilities
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        AL.setCurrentProcess(alCapabilities);

        // Set the distance model that will be used
        AL11.alDistanceModel(AL11.AL_INVERSE_DISTANCE);

        mALDev = device;
        mALCtx = ctx;

        setupSources();
        setMasterVolume(0.5f);

        mInitialized = true;

        LOGGER.info("Initialize AudioManager: " + mSources.size() + " sources available");
    }

    /** Attempt to create MAX_SOURCES sources */
    private void setupSources() {
        for (int i = 0; i < MAX_SOURCES; i++) {
            int source = AL11.alGenSources();
            if (AL11.alGetError() != AL11.AL_NO_ERROR) {
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
    private Source getAvailableSource() {
        for (Source s : mSources) {
            if (!s.isInUse()) {
                return s;
            }
        }
        return null;
    }

    /**
     * Attach an OpenAL source to all AudioSources in the list
     *
     * @param audioSources List of AudioSources to attach a source to
     */
    private void attachSources(List<Reference<AudioSource>> audioSources) {
        for (Reference<AudioSource> ref : audioSources) {
            AudioSource audioSource = ref.get();

            if (audioSource.getSource() == null) {
                Source source = getAvailableSource();
                assert source != null;

                audioSource.attachSource(source);
            }
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

    /**
     * Load a sound and give it an ID
     *
     * @param file .wav File to be loaded
     * @return The id of the loaded sound, or -1 if there was an error loading
     */
    public int loadSound(String file) {

        String[] searchPaths = {
            "engine/src/main/resources/audio/", "game/src/main/resources/audio/"
        };

        for (String p : searchPaths) {
            File f = new File(p + file).getAbsoluteFile();
            if (f.exists()) {
                return loadSound(f);
            }
        }
        return -1;
    }

    /**
     * Load a sound and give it an ID
     *
     * @param file .wav File to be loaded
     * @return The id of the loaded sound, or -1 if there was an error loading
     */
    public int loadSound(File file) {
        if (mALDev == -1) {
            return -1;
        }

        WaveSound sound = WaveSound.loadWav(file);

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

            // Don't attach a source if the AudioSource:
            //      Has no sound
            //      Has finished playing it's sound
            //      Is out of range of the listener
            if (audioSource.getSound() == null
                    || audioSource.getTimeLeft() < 0f
                    || distance > audioSource.getRadius()) {
                mAudioSources.remove(i);
                audioSource.detachSource();
                i--;
            }
        }

        // List is not sorted by priority for now but will be in the future so
        // detach all sources from index mSources.size() onwards
        if (mAudioSources.size() > mSources.size()) {
            detachSources(mAudioSources.subList(mSources.size(), mAudioSources.size()));
            attachSources(mAudioSources.subList(0, mSources.size()));
        } else {
            attachSources(mAudioSources);
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

    /** Get the AudioListener from the current scene */
    public void setAudioListener() {
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

        mMasterVolume = volume;
        AL11.alListenerf(AL11.AL_GAIN, mMasterVolume);
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
