/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio.components;

import lombok.experimental.Accessors;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.core.Reference;
import org.joml.Vector3f;
import org.lwjgl.openal.AL11;

@Accessors(prefix = "m")
public class AudioListener extends Component implements IFixedUpdate {

    /** Set the OpenAL listener's position to the position of mGameObject */
    private void updatePosition() {
        Vector3f pos = mGameObject.getTransform().getPosition();
        AL11.alListener3f(AL11.AL_POSITION, pos.x, pos.y, pos.z);
    }

    /** Set the orientation of the OpenAL listener to match the orientation of mGameObject */
    private void updateRotation() {
        Vector3f up = mGameObject.getTransform().getUpVector();
        Vector3f forward = mGameObject.getTransform().getForwardVector();

        AL11.alListenerfv(
                AL11.AL_ORIENTATION,
                new float[] {up.x, up.y, up.z, forward.x, forward.y, forward.z});
    }

    @Override
    protected void onDestroy() {
        // TODO: AudioListener cleanup
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (!AudioManager.getInstance().isInitialized()) {
            return;
        }

        // Only update position and rotation if this AudioListener is the currently active one
        Reference<AudioListener> curAudioListener = AudioManager.getInstance().getAudioListener();
        if (curAudioListener.isValid() && curAudioListener.get().equals(this)) {
            updatePosition();
            updateRotation();
        }
    }
}
