/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.misc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;

/**
 * Simple descriptor for a sound effect to play.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class SoundEffectDescriptor extends Component {
    /** Name of the sound to play. */
    @Getter @Setter private String mSoundName = "";

    @Override
    protected void onDestroy() {}
}
