package org.dragonskulle.audio.components;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;

public class AudioListener extends Component implements IFixedUpdate {

    @Override
    protected void onDestroy() {
        // TODO: AudioListener cleanup
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        // TODO: Follow GameObject's transform
    }
}
