/* (C) 2021 DragonSkulle */

package org.dragonskulle.audio.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.joml.Vector3f;
import org.lwjgl.openal.AL11;

@Accessors(prefix = "m")
public class AudioListener extends Component implements IFixedUpdate, IOnStart {

    private final Reference<AudioListener> mReference = this.getReference(AudioListener.class);
    @Getter private final Vector3f mPosition = new Vector3f();

    /** Set the OpenAL listener's position to the position of mGameObject. */
    private void updatePosition() {
        mGameObject.getTransform().getPosition(mPosition);
        AL11.alListener3f(AL11.AL_POSITION, mPosition.x, mPosition.y, mPosition.z);
    }

    /** Set the orientation of the OpenAL listener to match the orientation of mGameObject. */
    private void updateRotation() {
        Vector3f up = mGameObject.getTransform().getUpVector();
        Vector3f forward = mGameObject.getTransform().getForwardVector();

        AL11.alListenerfv(
                AL11.AL_ORIENTATION,
                new float[] {forward.x, forward.y, forward.z, up.x, up.y, up.z});
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        if (!AudioManager.getInstance().isInitialized()) {
            return;
        }

        // Only update position and rotation if this AudioListener is the currently active one
        assert mReference != null;
        if (mReference.equals(AudioManager.getInstance().getAudioListener())) {
            updatePosition();
            updateRotation();
        }
    }

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
    }
}
