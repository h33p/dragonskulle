/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.misc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;

@Accessors(prefix = "m")
public class SoundEffectDescriptor extends Component {
    @Getter @Setter private String mSoundName = "";

    @Override
    protected void onDestroy() {}
}
