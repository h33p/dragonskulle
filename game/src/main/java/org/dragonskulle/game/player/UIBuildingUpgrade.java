/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.network_data.StatData;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIFlatImage;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

/**
 * The UI Component to display the stats and upgradeable options for the selected building.
 *
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class UIBuildingUpgrade extends Component implements IOnStart, IFixedUpdate {
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    @Getter private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private Reference<GameObject> mBuildingUpgradeComponent;
    @Setter private Building mLastBuilding;
    private final HashMap<StatType, Reference<UIText>> mTextValueReferences = new HashMap<>();
    private UITextRect mAttackLevelText;
    private UITextRect mDefenceLevelText;
    private UITextRect mTokenGenerationText;

    /**
     * Constructor.
     *
     * @param mGetHexChosen the callback to get the hexagonTile selected
     * @param mGetPlayer    the callback to get the player from HumanPlayer
     */
    public UIBuildingUpgrade(
            UIMenuLeftDrawer.IGetHexChosen mGetHexChosen, UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetHexChosen = mGetHexChosen;
        this.mGetPlayer = mGetPlayer;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        // better way to do this dynamically
        mAttackLevelText = new UITextRect("0");
        mAttackLevelText.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        mDefenceLevelText = new UITextRect("0");
        mDefenceLevelText.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        mTokenGenerationText = new UITextRect("0");
        mTokenGenerationText.setRectTexture(GameUIAppearance.getInfoBox21Texture());

        mBuildingUpgradeComponent =
                getGameObject()
                        .buildChild(
                                "building_stats_upgrade",
                                new TransformUI(true),
                                (self) -> {
                                    UIManager manager = UIManager.getInstance();

                                    manager.buildHorizontalUI(
                                            self,
                                            0.05f,
                                            0.25f,
                                            0.45f,
                                            mAttackLevelText,
                                            mDefenceLevelText,
                                            mTokenGenerationText);

                                    manager.buildHorizontalUI(
                                            self,
                                            0.05f,
                                            0.45f,
                                            0.95f,
                                            (go) -> {
                                                go.getTransform(TransformUI.class)
                                                        .setPivotOffset(0.5f, 0f);
                                                UIButton but =
                                                        new UIButton(
                                                                (__, ___) ->
                                                                        purchaseUpgrade(
                                                                                StatType.ATTACK));
                                                but.setRectTexture(
                                                        GameUIAppearance.getSquareButtonTexture());
                                                go.addComponent(but);
                                                go.buildChild(
                                                        "sym",
                                                        new TransformUI(true),
                                                        (handle) -> {
                                                            handle.getTransform(TransformUI.class)
                                                                    .setParentAnchor(0.25f);
                                                            handle.addComponent(
                                                                    new UIFlatImage(
                                                                            new SampledTexture(
                                                                                    "ui/attack_symbol.png"),
                                                                            false));
                                                        });
                                            },
                                            (go) -> {
                                                go.getTransform(TransformUI.class)
                                                        .setPivotOffset(0.5f, 0f);
                                                UIButton but =
                                                        new UIButton(
                                                                (__, ___) ->
                                                                        purchaseUpgrade(
                                                                                StatType.DEFENCE));
                                                but.setRectTexture(
                                                        GameUIAppearance.getSquareButtonTexture());
                                                go.addComponent(but);
                                                go.buildChild(
                                                        "sym",
                                                        new TransformUI(true),
                                                        (handle) -> {
                                                            handle.getTransform(TransformUI.class)
                                                                    .setParentAnchor(0.25f);
                                                            handle.addComponent(
                                                                    new UIFlatImage(
                                                                            new SampledTexture(
                                                                                    "ui/defence_symbol.png"),
                                                                            false));
                                                        });
                                            },
                                            (go) -> {
                                                go.getTransform(TransformUI.class)
                                                        .setPivotOffset(0.5f, 0f);
                                                UIButton but =
                                                        new UIButton(
                                                                (__, ___) ->
                                                                        purchaseUpgrade(
                                                                                StatType
                                                                                        .TOKEN_GENERATION));
                                                but.setRectTexture(
                                                        GameUIAppearance.getSquareButtonTexture());
                                                go.addComponent(but);
                                                go.buildChild(
                                                        "sym",
                                                        new TransformUI(true),
                                                        (handle) -> {
                                                            handle.getTransform(TransformUI.class)
                                                                    .setParentAnchor(0.25f);
                                                            handle.addComponent(
                                                                    new UIFlatImage(
                                                                            new SampledTexture(
                                                                                    "ui/token_generation_symbol.png"),
                                                                            false));
                                                        });
                                            });
                                });
    }

    /**
     * Invoke a purchase for the selected stat.
     *
     * @param type the type
     */
    private void purchaseUpgrade(StatType type) {
        Reference<Player> player = mGetPlayer.getPlayer();
        if (Reference.isValid(player)) {
            Building building = mGetHexChosen.getHex().getBuilding();
            if (building != null) {
                player.get().getClientStatRequest().invoke(new StatData(building, type));
            }
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        HexagonTile tile = mGetHexChosen.getHex();
        if (tile != null) {
            Building building = tile.getBuilding();
            if (building != null && building.statsRequireVisualUpdate()) {
                StringBuilder builder = new StringBuilder("#Selected Building Stats \n");
                ArrayList<SyncStat> upgradeableStats = building.getUpgradeableStats();
                for (SyncStat upgradeableStat : upgradeableStats) {
                    Reference<UIText> labelText =
                            mTextValueReferences.get(upgradeableStat.getType());
                    if (Reference.isValid(labelText)) {
                        labelText.get().setText((Integer.toString(upgradeableStat.getLevel())));
                    } else {
                        switch (upgradeableStat.getType()) {
                            case ATTACK:
                                if (mAttackLevelText != null) {
                                    mTextValueReferences.put(
                                            StatType.ATTACK, mAttackLevelText.getLabelText());
                                }
                                break;
                            case DEFENCE:
                                if (mDefenceLevelText != null) {
                                    mTextValueReferences.put(
                                            StatType.DEFENCE, mDefenceLevelText.getLabelText());
                                }
                                break;
                            case TOKEN_GENERATION:
                                if (mTokenGenerationText != null) {
                                    mTextValueReferences.put(
                                            StatType.TOKEN_GENERATION,
                                            mTokenGenerationText.getLabelText());
                                }
                                break;
                        }
                    }
                }

                upgradeableStats.forEach(
                        s ->
                                builder.append(s.getType())
                                        .append(" -> ")
                                        .append(s.getValue())
                                        .append("\n"));
                building.setStatsRequireVisualUpdate(false);
                log.info(builder.toString());
            }
        }
    }
}
