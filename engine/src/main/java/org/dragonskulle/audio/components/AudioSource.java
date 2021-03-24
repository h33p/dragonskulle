package org.dragonskulle.audio.components;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnAwake;

public class AudioSource extends Component implements IFixedUpdate, IOnAwake {




    @Override
    protected void onDestroy() {
        // TODO: Stop playback and set source to not in use
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        // TODO: Update source position to follow GameObject's transform
    }

    @Override
    public void onAwake() {

    }
}
