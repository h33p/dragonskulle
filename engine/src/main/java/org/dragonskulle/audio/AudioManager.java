/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.core.Reference;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
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

    private final Map<Integer, WaveSound> mSoundMap = new HashMap<>();
    private final ArrayList<Source> mSources = new ArrayList<>();
    private final HashSet<Reference<AudioSource>> mAudioSources = new HashSet<>();
    private final ArrayList<Reference<AudioSource>> mActiveAudioSources = new ArrayList<>();

    private Reference<AudioListener> mAudioListener;
    private long mALDev = -1;
    private long mALCtx = -1;

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
        AL11.alDistanceModel(AL11.AL_LINEAR_DISTANCE_CLAMPED);

        mALDev = device;
        mALCtx = ctx;

        setupSources();

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
     * @param id ID for the sound
     * @param file .wav File to be loaded
     * @return true if the file was loaded successfully, false otherwise
     */
    public boolean loadSound(int id, String file) {
        if (mALDev == -1) {
            return false;
        }
        return loadSound(id, new File(file));
    }

    /**
     * Load a sound and give it an ID
     *
     * @param id ID for the sound
     * @param file .wav File to be loaded
     * @return true if the file was loaded successfully, false otherwise
     */
    public boolean loadSound(int id, File file) {
        if (mALDev == -1) {
            return false;
        }

        WaveSound sound = WaveSound.loadWav(file);

        if (sound == null) {
            return false;
        }

        sound.buffer = AL10.alGenBuffers();
        AL10.alBufferData(sound.buffer, sound.format, sound.data, sound.sampleRate);

        mSoundMap.put(id, sound);

        return true;
    }

    /**
     * Attempt to get a loaded sound by id
     *
     * @param id Integer of id that the sound was loaded with
     * @return A WaveSound object representing the sound, or null if there was no sound with that id
     */
    public WaveSound getSound(int id) {
        return mSoundMap.getOrDefault(id, null);
    }

    /**
     * Handles the distribution of sources between all of the AudioSources in the scene, assigning
     * sources to those that have the highest priority and removing them from those that no longer
     * need them
     */
    public void update() {
        // First remove any references to AudioSources that are no longer valid
        mAudioSources.removeIf(ref -> !ref.isValid());

        mActiveAudioSources.addAll(mAudioSources);

        // All references will be valid because they any invalid ones are removed at the start
        for (int i = 0; i < mActiveAudioSources.size(); i++) {
            AudioSource audioSource = mActiveAudioSources.get(i).get();

            // TODO: Distance check
            // TODO: Check audio time left (Need to calculate sound length first)

            if (audioSource.getSound() == null) {
                mActiveAudioSources.remove(i);
                audioSource.detachSource();
                i--;
            }
        }
        
        // List is not sorted by priority for now but will be in the future so
        // detach all sources from index mSources.size() onwards
        if (mActiveAudioSources.size() > mSources.size()) {
            detachSources(mActiveAudioSources.subList(mSources.size(), mActiveAudioSources.size()));
            attachSources(mActiveAudioSources.subList(0, mSources.size()));
        } else {
            attachSources(mActiveAudioSources);
        }

        mActiveAudioSources.clear();
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
     * Remove an audio source so that it will no longer have sources attached to it
     *
     * @param audioSource Reference to the AudioSource that will be removed
     */
    public void removeAudioSource(Reference<AudioSource> audioSource) {
        mAudioSources.remove(audioSource);
    }

    /**
     * Set the active AudioListener component for the scene. If multiple AudioListeners exist in one
     * scene, there is no way to know for certain which audioListener will actually be set as the
     * active listener and that behaviour should not be relied upon.
     *
     * @param audioListener Reference to the AudioListener component to be used
     */
    public void setAudioListener(Reference<AudioListener> audioListener) {
        if (audioListener.isValid()) {
            mAudioListener = audioListener;
        }
    }

    public void removeAudioListener(Reference<AudioListener> audioListener) {
        if (mAudioListener == audioListener) {
            mAudioListener = null;
        }
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

        for (WaveSound s : mSoundMap.values()) {
            AL11.alDeleteBuffers(s.buffer);
            s.data.clear();
        }
        mSoundMap.clear();

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
