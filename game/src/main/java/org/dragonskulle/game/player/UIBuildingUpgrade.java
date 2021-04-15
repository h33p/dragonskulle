/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIDropDown;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/**
 * @author Oscar L
 */
@Log
public class UIBuildingUpgrade extends Component implements IOnStart, IFrameUpdate {
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    private Reference<UIText> textReference;
    private Reference<GameObject> mBuildingUpgradeComponent;
    private Reference<GameObject> mStatChildren;

    public UIBuildingUpgrade(UIMenuLeftDrawer.IGetHexChosen mGetHexChosen) {
        this.mGetHexChosen = mGetHexChosen;
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {
    }

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {
        StringBuilder builder = new StringBuilder();
        final HexagonTile hexagonTile = mGetHexChosen.get();
        if (hexagonTile != null) {
            final Building building = hexagonTile.getBuilding();
            if (building != null) {
                buildStatUpgradeChildren(builder, building);
            }
        }
        if (Reference.isValid(textReference)) {
            textReference.get().setText(builder.toString());
        }
    }

    private void buildStatUpgradeChildren(StringBuilder builder, Building building) {
        List<SyncStat> stats = building.getStats();
        if (Reference.isValid(mStatChildren)) {
            mStatChildren.get().destroy();
        }
        mStatChildren =
                mBuildingUpgradeComponent
                        .get()
                        .buildChild(
                                "stats_upgrade_children",
                                new TransformUI(true),
                                (self) -> {
                                    self.addComponent(new UIText(
                                            "can this be seen?"));
                                    new UIDropDown(
                                            0,
                                            (drop) -> log.warning("will upgrade stat " + drop.getSelectedOption()),
                                            String.valueOf(stats.stream().map((stat) -> MessageFormat.format("{0}::{1}", stat.getClass().getSimpleName(), stat.getValue())).collect(Collectors.toList())));
                                });
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mBuildingUpgradeComponent =
                getGameObject()
                        .buildChild(
                                "upgrade_options",
                                new TransformUI(true),
                                (self) -> {
                                    final TransformUI transform =
                                            self.getTransform(TransformUI.class);
                                    transform.setParentAnchor(0.1f, 0.6f);
                                    transform.setMargin(0, -0.2f, 0, -0.2f);

                                    UIText mWindowText =
                                            new UIText( "PLACEHOLDER UPGRADE TEXT");
                                    textReference = mWindowText.getReference(UIText.class);
                                    self.addComponent(mWindowText);

                                    TransformUI textTransform =
                                            self.getTransform(TransformUI.class);
                                    textTransform.setMargin(0.2f, 0f, -0.2f, 0f);
                                });
    }

    private Component buildSingleStatChild(SyncStat stat, float offset) {

        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        // set transform depending on offset
        //        tran.setParentAnchor(0.3f, 0.4f + offset);
        return new UIStatUpgrader(stat, null); // TODO add actual method
    }
}
