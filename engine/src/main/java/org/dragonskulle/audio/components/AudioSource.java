/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.audio.Source;
import org.dragonskulle.audio.formats.Sound;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.Scene;
import org.joml.Vector3f;
import org.lwjgl.openal.AL11;

/**
 * Class representing a single audio source within the game.
 *
 * @author Harry Stoltz
 *     <p>By itself does nothing, but when playSound is called, it will be assigned an openAL source
 *     by the AudioManager and then will start playing sound. The sound is played from the location
 *     of the parent game object in the world.
 */
@Accessors(prefix = "m")
@Log
public class AudioSource extends Component implements IFixedUpdate, ILateFrameUpdate {

    private final Reference<AudioSource> mReference = getReference(AudioSource.class);
    @Getter private final Vector3f mPosition = new Vector3f();

    @Getter private Sound mSound = null;
    @Getter private Source mSource = null;
    @Getter private float mVolume = 1f;
    @Getter private float mPitch = 1f;
    @Getter private float mRadius = 50000f;
    @Getter private float mTimeLeft = -1f;
    @Getter private int mLooping = AL11.AL_FALSE;

    /** Update the position of the source to that of the GameObject. */
    private void updatePosition() {
        mGameObject.getTransform().getPosition(mPosition);

        if (mSource == null) {
            return;
        }

        AL11.alSource3f(
                mSource.getSource(), AL11.AL_POSITION, mPosition.x, mPosition.y, mPosition.z);
    }

    /**
     * Attach a Source to this AudioSource.
     *
     * @param source Source to be attached
     */
    public void attachSource(Source source) {
        if (source == null) {
            return;
        }

        mSource = source;

        mSource.setInUse(true);

        int s = mSource.getSource();

        AL11.alSourcef(s, AL11.AL_GAIN, mVolume);
        AL11.alSourcef(s, AL11.AL_PITCH, mPitch);
        AL11.alSourcef(s, AL11.AL_MAX_DISTANCE, mRadius);
        AL11.alSourcei(s, AL11.AL_LOOPING, mLooping);

        if (mSound != null) {
            AL11.alSourcei(s, AL11.AL_BUFFER, mSound.mBuffer);
        }
        updatePosition();

        AL11.alSourcePlay(s);
        log.finer("Attached source " + mSource.getSource());
    }

    /** Detach the Source from this AudioSource if there is one. */
    public void detachSource() {
        if (mSource == null) {
            return;
        }

        int source = mSource.getSource();

        AL11.alSourcef(source, AL11.AL_GAIN, 0f);
        AL11.alSourceStop(source);
        AL11.alSourcei(source, AL11.AL_BUFFER, 0);

        log.finer("Detached source " + mSource.getSource());
        mSource.setInUse(false);
        mSource = null;
    }

    /**
     * Set the volume of the AudioSource.
     *
     * @param volume Volume from 0f to 1f
     */
    public void setVolume(float volume) {
        volume = Math.max(0f, volume);
        volume = Math.min(volume, 1f);

        mVolume = volume;
        if (mSource != null) {
            AL11.alSourcef(mSource.getSource(), AL11.AL_GAIN, volume);
        }
    }

    /**
     * Set whether the AudioSource should loop.
     *
     * @param looping Whether to loop or not
     */
    public void setLooping(boolean looping) {
        mLooping = looping ? AL11.AL_TRUE : AL11.AL_FALSE;

        if (mSource != null) {
            AL11.alSourcei(mSource.getSource(), AL11.AL_LOOPING, mLooping);
        }
    }

    /**
     * Set the sound of this AudioSource via the name of the sound. It will be played as soon as the
     * AudioSource is given a source by the AudioManager.
     *
     * @param name Name of the sound to play
     */
    public void playSound(String name) {
        playSound(AudioManager.getResource(name));
    }

    /**
     * Set the sound of this AudioSource via a sound resource. It will be played as soon as the
     * AudioSource is given a source by the AudioManager.
     *
     * @param sound Sound resource to play
     */
    public void playSound(Resource<Sound> sound) {
        if (sound == null) {
            return;
        }

        mSound = sound.get();
        detachSource();
        mTimeLeft = mSound.mLength;
    }

    /**
     * Set the pitch of the sound.
     *
     * @param pitch Desired pitch (Value between 0f and 1f)
     */
    public void setPitch(float pitch) {
        pitch = Math.max(0f, pitch);
        pitch = Math.min(pitch, 1f);

        mPitch = pitch;
        if (mSource != null) {
            AL11.alSourcef(mSource.getSource(), AL11.AL_PITCH, mPitch);
        }
    }

    /**
     * Set the radius from the AudioSource that the sound can be heard from.
     *
     * @param radius New radius. Must be a positive value
     */
    public void setRadius(float radius) {
        radius = Math.max(0f, radius);

        mRadius = radius;
        if (mSource != null) {
            AL11.alSourcef(mSource.getSource(), AL11.AL_MAX_DISTANCE, mRadius);
        }
    }

    @Override
    protected void onDestroy() {
        detachSource();
        mSound = null;
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (Scene.getActiveScene() != Engine.getInstance().getPresentationScene()) {
            if (mSource != null) {
                detachSource();
            }
            return;
        }

        updatePosition();
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        AudioManager.getInstance().addAudioSource(mReference);
        if (mSource == null || mSound == null) {
            return;
        }

        mTimeLeft -= deltaTime;

        while (mLooping == AL11.AL_TRUE && mTimeLeft < 0f) {
            mTimeLeft += mSound.mLength;
        }

        if (mTimeLeft < 0f) {
            detachSource();
            mSound = null;
        }
    }
}
