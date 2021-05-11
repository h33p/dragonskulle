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

@Accessors(prefix = "m")
public class SoundEffectSource extends Component implements IOnStart, IFrameUpdate {

    private List<Reference<SoundEffectDescriptor>> mEffects = new ArrayList<>();
    private Reference<AudioSource> mSource;

    @Getter @Setter private float mSourceRange = 5f;

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

        if (source.getTimeLeft() <= 0) {
            Reference<SoundEffectDescriptor> desc =
                    mEffects.get((int) (Math.random() * mEffects.size()) % mEffects.size());

            if (Reference.isValid(desc)) {
                source.playSound(desc.get().getSoundName());
            }
        }
    }

    @Override
    protected void onDestroy() {}
}
