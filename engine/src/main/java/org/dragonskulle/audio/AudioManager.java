package org.dragonskulle.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The manager for the engine's audio system
 *
 * @author Harry Stoltz
 * <p>This class will handle the loading and buffering of all sound files, and will also manage a
 * pool of sources that can be used by AudioSources to play the sounds back
 */
public class AudioManager {
    private static final Logger LOGGER = Logger.getLogger("audio");
    private static final AudioManager AUDIO_MANAGER = new AudioManager();

    // TODO: Decide how many simultaneous audio sources we want to support
    private static final int MAX_SOURCES = 32;

    private final Map<Integer, WaveSound> mSoundMap = new HashMap<>();
    private final ArrayList<Source> mSources = new ArrayList<>();

    private long mALDev = -1;
    private long mALCtx = -1;

    private AudioManager() {

        // TODO: For now I'm just using the "best" device available, I have made AudioDevices.java
        //       which can be used to enumerate devices so we can allow user to choose in the future
        long device = ALC11.alcOpenDevice((ByteBuffer)null);
        if (device == 0L) {
            LOGGER.severe("Failed to open default OpenAL device, no audio will be available");
            return;
        }

        long ctx = ALC11.alcCreateContext(device, (IntBuffer)null);
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

        LOGGER.info("Initialize AudioManager: "
                + mSources.size() + " sources available");
    }

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
     * Cleanup all resources still in use
     */
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
