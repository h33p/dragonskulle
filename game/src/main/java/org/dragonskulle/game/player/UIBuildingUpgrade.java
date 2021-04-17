/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.network_data.StatData;
import org.dragonskulle.ui.*;

/** @author Oscar L */
@Log
@Accessors(prefix = "m")
public class UIBuildingUpgrade extends Component implements IOnStart, IFrameUpdate, IFixedUpdate {
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    @Getter private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private Reference<GameObject> mBuildingUpgradeComponent;
    private Reference<GameObject> mStatChildren;
    @Setter private Building mLastBuilding;
    private int ctr;
    private StatType selectedStatType = StatType.ATTACK;
    private Reference<UIButton> increaserReference;
    private Reference<GameObject> increaserGOReference;
    private String delimiter = " -- ";

    public UIBuildingUpgrade(
            UIMenuLeftDrawer.IGetHexChosen mGetHexChosen, UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetHexChosen = mGetHexChosen;
        this.mGetPlayer = mGetPlayer;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    @Override
    protected void onDestroy() {}

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
            if (building != null
                    && (!building.equals(mLastBuilding) || building.isDidStatsChange())) {
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
        String[] stats =
                building.getUpgradeableStats().stream()
                        .filter(Objects::nonNull)
                        .map(s -> s.getType().getNiceName() + delimiter + s.getValue())
                        .toArray(String[]::new);

        mStatChildren =
                mBuildingUpgradeComponent
                        .get()
                        .buildChild(
                                "stats_upgrade_children",
                                new TransformUI(true),
                                (self) -> {
                                    UIDropDown uiDropDown =
                                            new UIDropDown(
                                                    0,
                                                    (drop) -> {
                                                        selectedStatType =
                                                                StatType.valueFromNiceName(
                                                                        drop.getSelectedOption()
                                                                                .split(delimiter)[
                                                                                0]);
                                                        if (Reference.isValid(increaserReference)) {
                                                            increaserReference
                                                                    .get()
                                                                    .getLabelText()
                                                                    .get()
                                                                    .setText(
                                                                            "Increase for "
                                                                                    + mLastBuilding
                                                                                            .getStat(
                                                                                                    selectedStatType)
                                                                                            .getCost());
                                                        }
                                                    },
                                                    stats);
                                    uiDropDown.setOnOpen(
                                            () -> {
                                                log.info("running on open");
                                                if (Reference.isValid(increaserGOReference)) {
                                                    increaserGOReference.get().setEnabled(false);
                                                }
                                            });
                                    uiDropDown.setOnHide(
                                            () -> {
                                                if (Reference.isValid(increaserReference)) {
                                                    increaserGOReference.get().setEnabled(true);
                                                }
                                            });
                                    self.addComponent(uiDropDown);
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
                                new TransformUI(),
                                (self) -> {
                                    TransformUI transform = self.getTransform(TransformUI.class);
                                    transform.setPosition(0f, -0.1f);
                                    transform.setParentAnchor(0.02f, 0f);
                                    transform.setMargin(0f, -0.21f, -0f, 0.21f);

                                    increaserGOReference =
                                            self.buildChild(
                                                    " -- ",
                                                    new TransformUI(),
                                                    (go) -> {
                                                        UIButton increaser =
                                                                new UIButton(
                                                                        "default increaser",
                                                                        (button, __) -> {
                                                                            Reference<Player>
                                                                                    playerReference =
                                                                                            getGetPlayer()
                                                                                                    .get();
                                                                            if (Reference.isValid(
                                                                                    playerReference)) {
                                                                                playerReference
                                                                                        .get()
                                                                                        .getClientStatRequest()
                                                                                        .invoke(
                                                                                                new StatData(
                                                                                                        mLastBuilding,
                                                                                                        selectedStatType));
                                                                            }
                                                                        });

                                                        increaserReference =
                                                                increaser.getReference(
                                                                        UIButton.class);
                                                        TransformUI subtransform =
                                                                go.getTransform(TransformUI.class);
                                                        subtransform.setPosition(0f, 0.25f);
                                                        subtransform.setMargin(0f, 0.4f);
                                                        go.addComponent(increaser);
                                                    });
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
                    building.getStats()
                            .forEach(
                                    s ->
                                            builder.append(s.getType())
                                                    .append(" -> ")
                                                    .append(s.getValue())
                                                    .append("\n"));
                }
            }
            log.info(builder.toString());
            ctr = 0;
        }
    }
}
