/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.network_data.StatData;
import org.dragonskulle.ui.*;

/**
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class UIBuildingUpgrade extends Component implements IOnStart, IFrameUpdate, IFixedUpdate {
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    @Getter
    private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private Reference<GameObject> mBuildingUpgradeComponent;
    private Reference<GameObject> mStatChildren;
    @Setter
    private Building mLastBuilding;
    private int ctr;

    public UIBuildingUpgrade(UIMenuLeftDrawer.IGetHexChosen mGetHexChosen, UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetHexChosen = mGetHexChosen;
        this.mGetPlayer = mGetPlayer;
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
        final HexagonTile hexagonTile = mGetHexChosen.get();
        if (hexagonTile != null) {
            final Building building = hexagonTile.getBuilding();
            if (building != null && (!building.equals(mLastBuilding) || building.isDidStatsChange())) {
                log.info("bupdating");
                building.setDidStatsChange(false);
                mLastBuilding = building;
                if (Reference.isValid(mStatChildren)) {
                    mStatChildren.get().destroy();
                }
                buildStatUpgradeChildren(building);
            }
        }
    }

    private void buildStatUpgradeChildren(Building building) {
        List<SyncStat> stats = building.getStats();

        mStatChildren =
                mBuildingUpgradeComponent
                        .get()
                        .buildChild(
                                "stats_upgrade_children",
                                new TransformUI(true),
                                (self) -> {
                                    TransformUI transform = getGameObject().getTransform(TransformUI.class);
                                    SyncStat syncStat = stats.get(0);
                                    UIButton statValue = new UIButton("attack :: " + syncStat.getValue(),
                                            (button, __) -> {
                                                Reference<Player> playerReference = getGetPlayer().get();
                                                if (Reference.isValid(playerReference)) {
                                                    playerReference.get().getClientStatRequest().invoke(
                                                            new StatData(building, StatType.ATTACK)
                                                    );
                                                }
                                            });

//                                    UIDropDown uiDropDown = new UIDropDown(
//                                            0,
//                                            (drop) -> {
//                                                log.warning("will upgrade stat " + drop.getSelectedOption());
//                                                SyncStat selected = stats.get(drop.getSelected() - 1);
//                                                statValue.getLabelText().get().setText(selected.getClass().getSimpleName() + " :: " + selected.getValue());
//                                            },
//                                            "mAttack",
//                                            "mDefence",
//                                            "mTokenGeneration",
//                                            "mViewDistance",
//                                            "mAttackDistance");
                                    self.addComponent(statValue); // will display increaser next to dropdown
//                                    self.addComponent(uiDropDown);
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
                                    transform.setParentAnchor(0.05f, 0.01f);
                                    transform.setMargin(0.9f);
                                    UIText mWindowText =
                                            new UIText("PLACEHOLDER UPGRADE TEXT");
                                    self.addComponent(mWindowText);
                                });
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (ctr++ >= 100) {
            StringBuilder builder = new StringBuilder("#Selected Building Stats \n");
            HexagonTile tile = mGetHexChosen.get();
            if (tile != null) {
                Building building = tile.getBuilding();
                if (building != null) {
                    building.getStats().forEach(s -> builder.append(s.getType()).append(" -> ").append(s.getValue()).append("\n"));
                }
            }
            log.info(builder.toString());
            ctr = 0;
        }
    }
}
