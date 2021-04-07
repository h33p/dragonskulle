package org.dragonskulle.game.player;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/**
 * @author Oscar L
 */
public class UIStatUpgrader extends Component implements IOnStart, IFixedUpdate {
    private final SyncStat<?> stat;
    private final IUpgradeStat mStatIncreaserMethod;

    protected interface IUpgradeStat {
        void call();
    }

    public UIStatUpgrader(SyncStat<?> stat, IUpgradeStat statIncreaserMethod) {
        super();
        this.stat = stat;
        this.mStatIncreaserMethod = statIncreaserMethod;
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {

    }

    /**
     * Fixed update is aimed to be called a fixed number of times per second. The target rate is
     * defined by UPDATES_PER_SECOND in Engine.java Should be used for things that must be done at a
     * constant rate, such as physics, regardless of render-frame rate
     *
     * @param deltaTime Approximate time between calls to fixedUpdate
     */
    @Override
    public void fixedUpdate(float deltaTime) {

    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        UIText name = new UIText(
                new Vector3f(0f, 0f, 0f),
                Font.getFontResource(
                        "Rise of Kingdom.ttf"),
                stat.getClass().getSimpleName());
        UIRenderable upgradeGraphic = new UIRenderable(new SampledTexture("ui/upgrade_button.png"));
        UIButton upgradeButton = new UIButton((__, _____) -> mStatIncreaserMethod.call());
    }
}
