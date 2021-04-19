/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
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
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.renderer.SampledTexture;
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
    private StatType selectedStatType = StatType.ATTACK;
    private Reference<UIButton> increaserReference;
    private Reference<GameObject> increaserGOReference;
    private String delimiter = " -- ";
    private TransformUI transform;
    private Reference<UITextRect> defenceLevelTextRef;
    private Reference<UITextRect> attackLevelTextRef;
    private Reference<UITextRect> tokenGenerationLevelTextRef;
    private final ArrayList<Reference<UITextRect>> mTextValueReferences = new ArrayList<>();

    public UIBuildingUpgrade(
            UIMenuLeftDrawer.IGetHexChosen mGetHexChosen, UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetHexChosen = mGetHexChosen;
        this.mGetPlayer = mGetPlayer;
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
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
                    && (!building.equals(mLastBuilding) || building.statsRequireVisualUpdate())) {
                building.setStatsRequireVisualUpdate(false);
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
                        .toArray(String[]::new); //need to link this to upgrade menu


    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mStatChildren =
                getGameObject()
                        .buildChild(
                                "building_upgrade",
                                new TransformUI(true),
                                (self) -> {
                                    transform = self.getTransform(TransformUI.class);
                                    UIManager manager = UIManager.getInstance();
                                    manager.buildHorizontalUI(self, 0.08f,
                                            0.25f,
                                            0.65f,
                                            new UIFlatImage(new SampledTexture("ui/slider.png")),//"ui/attack_symbol.png")),
                                            new UIFlatImage(new SampledTexture("ui/slider.png")),//ui/defense_symbol.png")),
                                            new UIFlatImage(new SampledTexture("ui/slider.png"))//ui/token_generation_symbol.png"))
                                    );
                                    //better way to do this dynamiucally
                                    UITextRect attackLevelText = new UITextRect("0");
                                    UITextRect defenceLevelText = new UITextRect("0");
                                    UITextRect tokenGenerationText = new UITextRect("0");
                                    attackLevelTextRef = attackLevelText.getReference(UITextRect.class);
//                                    mTextValueReferences.add(attackLevelTextRef);
                                    defenceLevelTextRef = defenceLevelText.getReference(UITextRect.class);
//                                    mTextValueReferences.add(defenceLevelTextRef);
                                    tokenGenerationLevelTextRef = tokenGenerationText.getReference(UITextRect.class);
//                                    mTextValueReferences.add(tokenGenerationLevelTextRef);
                                    manager.buildHorizontalUI(self, 0.08f,
                                            0.65f,
                                            0.85f,
                                            attackLevelText,
                                            defenceLevelText,
                                            tokenGenerationText
                                    );

                                    manager.buildHorizontalUI(self, 0.08f,
                                            0.95f,
                                            1.15f,
                                            new UIButton("+", (__, ___) -> log.info("will increase attack stat")),
                                            new UIButton("+", (__, ___) -> log.info("will increase defence stat")),
                                            new UIButton("+", (__, ___) -> log.info("will increase token generation stat"))
                                    );

                                });
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        HexagonTile tile = mGetHexChosen.get();
        if (tile != null) {
            Building building = tile.getBuilding();
            if (building != null && building.statsRequireVisualUpdate()) {
                StringBuilder builder = new StringBuilder("#Selected Building Stats \n");
                ArrayList<SyncStat> upgradeableStats = building.getUpgradeableStats();
//                for (int i = 0; i < upgradeableStats.size(); i++) {
//                    Reference<UITextRect> textRef = mTextValueReferences.get(i);
//                    if (Reference.isValid(textRef)) {
//                        textRef.get().getLabelText().get().setText((Integer.toString(upgradeableStats.get(i).getLevel())));
//                    }
//                }

                upgradeableStats
                        .forEach(
                                s ->
                                        builder.append(s.getType())
                                                .append(" -> ")
                                                .append(s.getValue())
                                                .append("\n"));
                log.info(builder.toString());
            }
        }
    }
}