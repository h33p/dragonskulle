/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.misc;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;

/**
 * Simple sound effect player.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class SoundEffectSource extends Component implements IOnStart, IFrameUpdate {

    /** List of sound effect descriptors to choose from. */
    private List<Reference<SoundEffectDescriptor>> mEffects = new ArrayList<>();
    /** Audio source to play from. */
    private Reference<AudioSource> mSource;

    /** Effect source range. */
    @Getter @Setter private float mSourceRange = 4f;

    /** Should only one sound effect play. */
    @Getter @Setter private boolean mPlayOnce = false;

    /** Did play the sound effect. */
    private boolean mPlayed = false;

    /** Global disable for all sound effects. */
    @Accessors(prefix = "s")
    @Getter
    @Setter
    private static boolean sGlobalDisable = false;

    @Override
    public void onStart() {
        getGameObject().getComponents(SoundEffectDescriptor.class, mEffects);
        AudioSource source = new AudioSource();
        source.setRadius(mSourceRange);
        getGameObject().addComponent(source);
        mSource = source.getReference(AudioSource.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (!Reference.isValid(mSource) || mEffects.size() == 0) {
            return;
        }

        AudioSource source = mSource.get();

        if (source.getTimeLeft() <= 0 && (!mPlayOnce || mPlayed) && !sGlobalDisable) {
            playEffect();
        }
    }

    /** Play a sound effect. */
    public void playEffect() {
        if (!Reference.isValid(mSource)) {
            return;
        }

        AudioSource source = mSource.get();

        Reference<SoundEffectDescriptor> desc =
                mEffects.get((int) (Math.random() * mEffects.size()) % mEffects.size());

        if (Reference.isValid(desc)) {
            source.playSound(desc.get().getSoundName());
            mPlayed = true;
        }
    }

    @Override
    protected void onDestroy() {}
}
