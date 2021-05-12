/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.apache.commons.io.FilenameUtils;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.audio.formats.Sound;
import org.dragonskulle.audio.formats.WaveSound;
import org.dragonskulle.components.Transform;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.dragonskulle.core.Scene;
import org.dragonskulle.settings.Settings;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

/**
 * The manager for the engine's audio system.
 *
 * @author Harry Stoltz
 *     <p>This class will handle the loading and buffering of all sound files, and will also manage
 *     a pool of sources that can be used by AudioSources to play the sounds back
 */
@Accessors(prefix = "m")
@Log
public class AudioManager {
    private static final AudioManager AUDIO_MANAGER = new AudioManager();

    // TODO: Decide how many simultaneous audio sources we want to support
    private static final int MAX_SOURCES = 32;

    private final ArrayList<Sound> mSounds = new ArrayList<>();
    private final ArrayList<Source> mSources = new ArrayList<>();
    private final HashSet<Reference<AudioSource>> mAudioSources = new HashSet<>();
    public static final String SETTINGS_VOLUME_STRING = "masterVolume";
    public static final String SETTINGS_MUTE_STRING = "masterMuted";

    @Getter private Reference<AudioListener> mAudioListener;
    private long mAlDev = -1;
    private long mAlCtx = -1;

    @Getter private float mMasterVolume = 1f;

    @Getter
    private boolean mMasterMuted =
            Settings.getInstance().retrieveBoolean(SETTINGS_MUTE_STRING, false);

    @Getter private boolean mInitialized = false;

    static {
        ResourceManager.registerResource(
                Sound.class,
                (args) -> String.format("audio/%s", args.getName()),
                (buffer, args) -> {
                    final String extension = FilenameUtils.getExtension(args.getName());
                    switch (extension) {
                        case "wav":
                            return new WaveSound(buffer);
                        default:
                            log.warning("Attempted to load unsupported audio file");
                            return null;
                    }
                });
    }

    /**
     * Get a sound resource from the resource manager.
     *
     * @param name Name of the resource to get
     * @return The resource object if we got it, or null
     */
    public static Resource<Sound> getResource(String name) {
        return ResourceManager.getResource(Sound.class, name);
    }

    /**
     * Constructor for AudioManager. It's private as AudioManager is designed as a singleton. Opens
     * an OpenAL device and creates and sets the context for the process. Also attempts to create up
     * to MAX_SOURCES sources
     */
    private AudioManager() {
        initAudioManager();
    }

    /** Attempt to create MAX_SOURCES sources. */
    private void setupSources() {
        for (int i = 0; i < MAX_SOURCES; i++) {
            int source = AL11.alGenSources();
            int error = AL11.alGetError();
            if (error != AL11.AL_NO_ERROR) {
                switch (error) {
                    case AL11.AL_OUT_OF_MEMORY:
                        log.warning("Error whilst creating sources (AL_OUT_OF_MEMORY)");
                        break;
                    case AL11.AL_INVALID_VALUE:
                        log.warning("Error whilst creating sources (AL_INVALID_VALUE)");
                        break;
                    case AL11.AL_INVALID_OPERATION:
                        log.warning("Error whilst creating sources (AL_INVALID_OPERATION)");
                        break;
                    default:
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
    private Source getAvailableSource() {
        for (Source s : mSources) {
            if (!s.isInUse()) {
                return s;
            }
        }
        return null;
    }

    /**
     * Attach an OpenAL source to all AudioSources in the list.
     *
     * @param audioSources List of AudioSources to attach a source to
     */
    private void attachSources(List<Reference<AudioSource>> audioSources) {
        for (Reference<AudioSource> ref : audioSources) {
            AudioSource audioSource = ref.get();

            if (audioSource.getSource() == null) {
                Source source = getAvailableSource();
                if (source == null) {
                    break;
                }

                audioSource.attachSource(source);
            }
        }
    }

    /**
     * Detach the OpenAL source from each AudioSource in the list.
     *
     * @param audioSources List of AudioSources to detach a source from
     */
    private void detachSources(List<Reference<AudioSource>> audioSources) {
        for (Reference<AudioSource> ref : audioSources) {
            ref.get().detachSource();
        }
    }

    /**
     * Initialise the audio manager by opening a device, creating a context and then creating as
     * many sources as possible.
     */
    public void initAudioManager() {
        if (mAlDev != -1 || mAlCtx != -1) {
            return;
        }

        // TODO: For now I'm just using the "best" device available, I have made AudioDevices.java
        //       which can be used to enumerate devices so we can allow user to choose in the future
        long device = ALC11.alcOpenDevice((ByteBuffer) null);
        if (device == 0L) {
            log.severe("Failed to open default OpenAL device, no audio will be available");
            return;
        }

        long ctx = ALC11.alcCreateContext(device, (IntBuffer) null);
        if (!ALC11.alcMakeContextCurrent(ctx)) {
            log.severe("Failed to set OpenAL context, no audio will be available");
            ALC11.alcCloseDevice(device);
            return;
        }

        // Get and set OpenAL capabilities
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        AL.setCurrentProcess(alCapabilities);

        // Set the distance model that will be used
        AL11.alDistanceModel(AL11.AL_INVERSE_DISTANCE);

        mAlDev = device;
        mAlCtx = ctx;

        setupSources();
        Settings settings = Settings.getInstance();
        float volume = settings.retrieveFloat(SETTINGS_VOLUME_STRING, 0.5f);
        setMasterVolume(volume);

        mInitialized = true;

        log.fine("Initialize AudioManager: " + mSources.size() + " sources available");
    }

    /**
     * Updates the OpenAL listener position and rotation from the singleton audio listener in the
     * scene.
     */
    private void updateListenerPosAndRot() {

        Transform t = mAudioListener.get().getGameObject().getTransform();

        Vector3f pos = t.getPosition();
        AL11.alListener3f(AL11.AL_POSITION, pos.x, pos.y, pos.z);

        Vector3f up = t.getUpVector();
        Vector3f forward = t.getForwardVector();

        AL11.alListenerfv(
                AL11.AL_ORIENTATION,
                new float[] {forward.x, forward.y, forward.z, up.x, up.y, up.z});
    }

    /**
     * Handles the distribution of sources between all of the AudioSources in the scene, assigning
     * sources to those that have the highest priority and removing them from those that no longer
     * need them.
     */
    public void update() {
        if (!mInitialized || !Reference.isValid(mAudioListener)) {
            return;
        }

        ArrayList<Reference<AudioSource>> audioSources = new ArrayList<>(mAudioSources);

        updateListenerPosAndRot();

        // remove any references to AudioSources that are no longer valid
        audioSources.removeIf(Reference::isInvalid);

        // All references will be valid because they any invalid ones are removed at the start
        for (int i = 0; i < audioSources.size(); i++) {
            AudioSource audioSource = audioSources.get(i).get();

            // Get the distance of the source from the listener
            float distance = 100000f;
            if (Reference.isValid(mAudioListener)) {
                distance =
                        mAudioListener
                                .get()
                                .getGameObject()
                                .getTransform()
                                .getPosition()
                                .distance(audioSource.getPosition());
            }

            // Don't attach a source if the AudioSource:
            //      Has no sound
            //      Has finished playing it's sound
            //      Is out of range of the listener
            if (audioSource.getSound() == null
                    || audioSource.getTimeLeft() < 0f
                    || distance > audioSource.getRadius()) {
                audioSources.remove(i);
                audioSource.detachSource();
                i--;
            }
        }

        if (audioSources.size() > mSources.size()) {
            detachSources(audioSources.subList(mSources.size(), mAudioSources.size()));
            attachSources(audioSources.subList(0, mSources.size()));
        } else {
            attachSources(audioSources);
        }

        mAudioSources.clear();
    }

    /**
     * Add an audio source so that it can have a source attached to it when required.
     *
     * @param audioSource Reference to the AudioSource that will be added
     */
    public void addAudioSource(Reference<AudioSource> audioSource) {
        mAudioSources.add(audioSource);
    }

    /**
     * Get the singleton AudioListener component from the currently active scene and set it as the
     * active AudioListener.
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
     * Set the master volume for the game.
     *
     * @param volume Value between 0f and 1f
     */
    public void setMasterVolume(float volume) {
        volume = Math.max(0f, volume);
        volume = Math.min(volume, 1f);

        mMasterVolume = volume;
        AL11.alListenerf(AL11.AL_GAIN, mMasterVolume);

        // Ensure the volume stays either muted or unmuted.
        setMasterMute(mMasterMuted);
    }

    /**
     * Set the master mute value.
     *
     * @param muted New value of master mute.
     */
    public void setMasterMute(boolean muted) {
        if (muted) {
            mMasterMuted = true;
            AL11.alListenerf(AL11.AL_GAIN, 0f);
        } else {
            mMasterMuted = false;
            AL11.alListenerf(AL11.AL_GAIN, mMasterVolume);
        }
        Settings.getInstance().saveValue(SETTINGS_MUTE_STRING, muted, true);
    }

    /** Toggles the master mute. */
    public void toggleMasterMute() {
        setMasterMute(!mMasterMuted);
    }

    /** Cleanup all resources still in use. */
    public void cleanup() {
        if (mAlDev == -1) {
            return;
        }

        for (Source s : mSources) {
            AL11.alDeleteSources(s.getSource());
        }
        mSources.clear();

        for (Sound s : mSounds) {
            AL11.alDeleteBuffers(s.mBuffer);
        }
        mSounds.clear();

        ALC11.alcMakeContextCurrent(0L);
        ALC11.alcDestroyContext(mAlCtx);
        ALC11.alcCloseDevice(mAlDev);
        mAlDev = -1;
        mAlCtx = -1;
    }

    /**
     * Get the singleton instance of the audio manager.
     *
     * @return AudioManager instance
     */
    public static AudioManager getInstance() {
        return AUDIO_MANAGER;
    }
}
