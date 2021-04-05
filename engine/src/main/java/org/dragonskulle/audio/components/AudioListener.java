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
public class AudioListener extends Component implements IOnStart {

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
    }
}
