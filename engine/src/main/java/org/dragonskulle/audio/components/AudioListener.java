/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio.components;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Scene;

@Accessors(prefix = "m")
public class AudioListener extends Component implements IOnStart {

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
    }
}
