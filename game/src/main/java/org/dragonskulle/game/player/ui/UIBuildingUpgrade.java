/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
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
    @Getter(AccessLevel.PROTECTED)
    private final UIShopSection mParent;

    private final HashMap<StatType, Reference<UITextRect>> mLevelTexts = new HashMap<>();
    private final HashMap<StatType, Reference<UITextRect>> mCostTexts = new HashMap<>();
    private UIMenuLeftDrawer.IGetBuildingChosen mGetBuildingChosen;
    private UIMenuLeftDrawer.IUpdateBuildingChosen mUpdateBuildingSelected;
    private Building mLastBuilding = null;
    @Getter @Setter private int mBuildingStatUpdateCount;
    private final HashMap<StatType, Reference<UIButton>> mUpgradeButtonRefs = new HashMap<>();
    private Reference<GameObject> mCostLabel;

    /**
     * Constructor.
     *
     * @param mParent its parent to be lazy and use its IParams
     */
    public UIBuildingUpgrade(UIShopSection mParent) {
        this.mParent = mParent;
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
        mGetBuildingChosen = getParent().getParent().mGetBuildingChosen;
        mUpdateBuildingSelected = getParent().getParent().mUpdateBuildingSelected;

        createReferences(mLevelTexts);
        createReferences(mCostTexts);
        getGameObject()
                .buildChild(
                        "building_stats_upgrade",
                        new TransformUI(true),
                        (self) -> {
                            UIManager manager = UIManager.getInstance();

                            manager.buildHorizontalUI(
                                    self, 0.05f, 0.2f, 0.4f, unpackReferences(mLevelTexts));

                            manager.buildHorizontalUI(
                                    self,
                                    0.05f,
                                    0.4f,
                                    0.9f,
                                    buildStatUpgrade(StatType.ATTACK, "ui/attack_symbol.png"),
                                    buildStatUpgrade(StatType.DEFENCE, "ui/defence_symbol.png"),
                                    buildStatUpgrade(
                                            StatType.TOKEN_GENERATION,
                                            "ui/token_generation_symbol.png"));

                            mCostLabel =
                                    self.buildChild(
                                            "cost_label",
                                            new TransformUI(true),
                                            g -> {
                                                TransformUI tran =
                                                        g.getTransform(TransformUI.class);
                                                tran.setParentAnchor(0.3f, 0.38f);
                                                tran.setPosition(0f, 0.145f);
                                                g.addComponent(new UIText("COST"));
                                            });

                            manager.buildHorizontalUI(
                                    self, 0.05f, 0.65f, 1.15f, unpackReferences(mCostTexts));
                        });
    }

    /**
     * Unpacks the references in the src map, then returns an array of the {@link
     * org.dragonskulle.ui.UIManager.IUIBuildHandler}'s in the {@link UITextRect}.
     *
     * @param src the src
     * @return an array of the {@link org.dragonskulle.ui.UIManager.IUIBuildHandler}'s
     */
    private UIManager.IUIBuildHandler[] unpackReferences(
            HashMap<StatType, Reference<UITextRect>> src) {
        return new UIManager.IUIBuildHandler[] {
            src.get(StatType.ATTACK).get(),
            src.get(StatType.DEFENCE).get(),
            src.get(StatType.TOKEN_GENERATION).get()
        };
    }

    /**
     * Creates a {@link UITextRect} with aspect ration 2:1, then stores a reference in the
     * destination map.
     *
     * @param dest the destination map.
     */
    private void createReferences(HashMap<StatType, Reference<UITextRect>> dest) {
        for (StatType stat : Building.getDefaultShopStats()) {
            UITextRect ui = create21TextRect();
            dest.put(stat, ui.getReference(UITextRect.class));
        }
    }

    /**
     * Creates a @{link UITextRect} with a 2:1 aspect ratio and no text.
     *
     * @return the UITextRect Component
     */
    private static UITextRect create21TextRect() {
        UITextRect rect = new UITextRect("");
        rect.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        return rect;
    }

    /**
     * Build a stat upgrader with a custom texture.
     *
     * @param type the stat type
     * @param textureName the texture file path
     * @return the builder
     */
    private UIManager.IUIBuildHandler buildStatUpgrade(StatType type, String textureName) {
        return (go) -> {
            go.getTransform(TransformUI.class).setPivotOffset(0.5f, 0f);
            UIButton but = new UIButton((__, ___) -> purchaseUpgrade(type));
            but.setRectTexture(GameUIAppearance.getSquareButtonTexture());
            go.addComponent(but);
            go.buildChild(
                    "sym",
                    new TransformUI(true),
                    (handle) -> {
                        handle.getTransform(TransformUI.class).setParentAnchor(0.25f);
                        handle.addComponent(
                                new UIFlatImage(new SampledTexture(textureName), false));
                    });

            mUpgradeButtonRefs.put(type, but.getReference(UIButton.class));
        };
    }

    /**
     * Invoke a purchase for the selected stat.
     *
     * @param type the type
     */
    private void purchaseUpgrade(StatType type) {
        Reference<Player> player =
                getParent().getParent().mGetPlayer.getPlayer().getReference(Player.class);
        if (Reference.isValid(player)) {
            HexagonTile hex = getParent().getParent().mGetHexChosen.getHex();
            if (hex != null) {
                Building building = hex.getBuilding();
                if (building != null) {
                    player.get().getClientStatRequest().invoke(new StatData(building, type));
                }
            }
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (getParent().didBuild() && mUpdateBuildingSelected != null) {
            mUpdateBuildingSelected.update();
        }
        if (mGetBuildingChosen != null) {
            Reference<Building> buildingRef = mGetBuildingChosen.getBuilding();
            if (Reference.isValid(buildingRef)) {
                Building building = buildingRef.get();
                int statUpdateCount = building.getStatUpdateCount();
                hideMaxStats(building);
                if (statUpdateCount > getBuildingStatUpdateCount()
                        || !building.equals(mLastBuilding)) {
                    setBuildingStatUpdateCount(statUpdateCount);
                    getParent().markDidBuild(false);
                    mLastBuilding = building;
                    building.getShopStats().forEach(this::updateVisibleStat);
                }
            }
        }
    }

    /**
     * Hides the {@link #mCostLabel} if all the stats in the shop are their max value.
     *
     * @param building the building the shop is looking at
     */
    private void hideMaxStats(Building building) {
        // This is atomic as its being used inside a forEach loop.
        AtomicInteger numberOfMaxStats = new AtomicInteger();
        building.getShopStats()
                .forEach(
                        (s) -> {
                            Reference<UIButton> uiButtonReference =
                                    mUpgradeButtonRefs.get(s.getType());
                            if (Reference.isValid(uiButtonReference)) {
                                boolean maxLevel = s.isMaxLevel();
                                if (maxLevel) numberOfMaxStats.getAndIncrement();
                                uiButtonReference.get().setEnabled(!maxLevel);
                            }
                        });

        if (Reference.isValid(mCostLabel)) {
            mCostLabel
                    .get()
                    .setEnabled(numberOfMaxStats.get() != Building.getNumberOfDefaultShopStats());
        }
    }

    /**
     * Updates the SyncStat values and costs shown in the shop. It will also disable upgrading of
     * any max level buildings.
     *
     * @param upgradeableStat the upgradeable stat
     */
    private void updateVisibleStat(SyncStat upgradeableStat) {
        setLevelTextRef(upgradeableStat);
        setCostTextRef(upgradeableStat);
    }

    /**
     * Sets the text reference which represents the cost with the SyncStats cost.
     *
     * @param upgradeableStat the stat to update
     */
    private void setCostTextRef(SyncStat upgradeableStat) {
        Reference<UITextRect> textRef = mCostTexts.get(upgradeableStat.getType());
        if (Reference.isValid(textRef)) {
            UITextRect rect = textRef.get();
            if (upgradeableStat.isMaxLevel()) {
                rect.getGameObject().setEnabled(false);
            } else {
                rect.getGameObject().setEnabled(true);
                setTextOnRef(rect.getLabelText(), (String.valueOf(upgradeableStat.getCost())));
            }
        }
    }

    /**
     * Sets the text reference which represents the level with the SyncStats cost.
     *
     * @param upgradeableStat the stat to update
     */
    private void setLevelTextRef(SyncStat upgradeableStat) {
        Reference<UITextRect> textRef = mLevelTexts.get(upgradeableStat.getType());
        if (Reference.isValid(textRef)) {
            setTextOnRef(textRef.get().getLabelText(), String.valueOf(upgradeableStat.getLevel()));
        }
    }

    /**
     * Sets text on the object if the reference is valid.
     *
     * @param textRef the reference to the {@link UIText}
     * @param value the string to be set
     */
    private void setTextOnRef(Reference<UIText> textRef, String value) {
        if (Reference.isValid(textRef)) {
            textRef.get().setText(value);
        }
    }
}
