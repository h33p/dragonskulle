/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.HashMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
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
import org.lwjgl.CLongBuffer;

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

    private final HashMap<StatType, Reference<UIText>> mTextValueReferences = new HashMap<>();
    private UITextRect mAttackLevelText;
    private UITextRect mDefenceLevelText;
    private UITextRect mTokenGenerationText;
    private UITextRect mAttackCostText;
    private UITextRect mDefenceCostText;
    private UITextRect mTokenGenerationCostText;
    private final HashMap<StatType, Reference<UIText>> mTextCostReferences = new HashMap<>();
    private UIMenuLeftDrawer.IGetBuildingChosen mGetBuildingChosen;
    private UIMenuLeftDrawer.IUpdateBuildingChosen mUpdateBuildingSelected;
    private Building mLastBuilding = null;
    @Getter
    @Setter
    private int mBuildingStatUpdateCount;
    private final HashMap<StatType, Reference<UIButton>> mUpgradeButtonRefs = new HashMap<>();

    /**
     * Constructor.
     *
     * @param mParent its parent to be lazy and use its IParams
     */
    public UIBuildingUpgrade(UIShopSection mParent) {
        this.mParent = mParent;
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy.
     */
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
        String attackVal = "";
        String defenceVal = "";
        String tokenGenVal = "";
        String attackCost = "";
        String defenceCost = "";
        String tokenGenCost = "";
        Reference<Building> buildingRef = mGetBuildingChosen.getBuilding();
        if (Reference.isValid(buildingRef)) {
            Building building = buildingRef.get();
            attackVal = String.valueOf(building.getStat(StatType.ATTACK).getValue());
            defenceVal = String.valueOf(building.getStat(StatType.DEFENCE).getValue());
            tokenGenVal = String.valueOf(building.getStat(StatType.TOKEN_GENERATION).getValue());

            attackCost = String.valueOf(building.getStat(StatType.ATTACK).getCost());
            defenceCost = String.valueOf(building.getStat(StatType.DEFENCE).getCost());
            tokenGenCost = String.valueOf(building.getStat(StatType.TOKEN_GENERATION).getCost());
        }
        // better way to do this dynamically
        mAttackLevelText = new UITextRect(attackVal);
        mAttackLevelText.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        mDefenceLevelText = new UITextRect(defenceVal);
        mDefenceLevelText.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        mTokenGenerationText = new UITextRect(tokenGenVal);
        mTokenGenerationText.setRectTexture(GameUIAppearance.getInfoBox21Texture());

        mTextValueReferences.put(StatType.ATTACK, mAttackLevelText.getLabelText());
        mTextValueReferences.put(StatType.DEFENCE, mDefenceLevelText.getLabelText());
        mTextValueReferences.put(StatType.TOKEN_GENERATION, mTokenGenerationText.getLabelText());

        mAttackCostText = new UITextRect(attackCost);
        mAttackCostText.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        mDefenceCostText = new UITextRect(defenceCost);
        mDefenceCostText.setRectTexture(GameUIAppearance.getInfoBox21Texture());
        mTokenGenerationCostText = new UITextRect(tokenGenCost);
        mTokenGenerationCostText.setRectTexture(GameUIAppearance.getInfoBox21Texture());

        mTextCostReferences.put(StatType.ATTACK, mAttackCostText.getLabelText());
        mTextCostReferences.put(StatType.DEFENCE, mDefenceCostText.getLabelText());
        mTextCostReferences.put(StatType.TOKEN_GENERATION, mTokenGenerationCostText.getLabelText());

        getGameObject()
                .buildChild(
                        "building_stats_upgrade",
                        new TransformUI(true),
                        (self) -> {
                            UIManager manager = UIManager.getInstance();

                            manager.buildHorizontalUI(
                                    self,
                                    0.05f,
                                    0.2f,
                                    0.4f,
                                    mAttackLevelText,
                                    mDefenceLevelText,
                                    mTokenGenerationText);

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

                            self.buildChild(
                                    "cost_label",
                                    new TransformUI(true),
                                    g -> {
                                        TransformUI tran = g.getTransform(TransformUI.class);
                                        tran.setParentAnchor(0.3f, 0.38f);
                                        tran.setPosition(0f, 0.145f);
                                        g.addComponent(new UIText("COST"));
                                    });
                            manager.buildHorizontalUI(
                                    self,
                                    0.05f,
                                    0.65f,
                                    1.15f,
                                    mAttackCostText,
                                    mDefenceCostText,
                                    mTokenGenerationCostText);
                        });
    }

    /**
     * Build a stat upgrader with a custom texture.
     *
     * @param type        the stat type
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
        Reference<Player> player = getParent().getParent().mGetPlayer.getPlayer();
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
                if (statUpdateCount > getBuildingStatUpdateCount()
                        || !building.equals(mLastBuilding)) {
                    setBuildingStatUpdateCount(statUpdateCount);
                    getParent().markDidBuild(false);
                    mLastBuilding = building;
                    building.getUpgradeableStats(true).forEach(this::updateVisibleStat);
                }
            }
        }
    }

    /**
     * Updates the SyncStat values and costs shown in the shop. It will also disable upgrading of any max level buildings.
     *
     * @param upgradeableStat the upgradeable stat
     */
    private void updateVisibleStat(SyncStat upgradeableStat) {
        if (!upgradeableStat.isUpgradeable()) disableFurtherUpgrade(upgradeableStat);
        setValueTextRef(upgradeableStat, mTextValueReferences.get(upgradeableStat.getType()));
        setCostTextRef(upgradeableStat, mTextCostReferences.get(upgradeableStat.getType()));
    }

    /**
     * Disables the button to upgrade the stat further.
     *
     * @param upgradeableStat the upgradeable stat which is at max level
     */
    private void disableFurtherUpgrade(SyncStat upgradeableStat) {
        Reference<UIButton> uiButtonReference = mUpgradeButtonRefs.get(upgradeableStat.getType());
        if (Reference.isValid(uiButtonReference)) uiButtonReference.get().setEnabled(false);
    }

    /**
     * Sets the text reference which represents the cost with the SyncStats cost.
     *
     * @param upgradeableStat the stat to update
     * @param textRef         the reference to the object
     */
    private void setCostTextRef(SyncStat upgradeableStat, Reference<UIText> textRef) {
        if (Reference.isInvalid(textRef)) {
            textRef = ensureCostTextRef(upgradeableStat);
        }
        setTextOnRef(textRef, (String.valueOf(upgradeableStat.getCost())));
    }

    /**
     * Sets the text reference which represents the level with the SyncStats cost.
     *
     * @param upgradeableStat the stat to update
     * @param textRef         the reference to the object
     */
    private void setValueTextRef(SyncStat upgradeableStat, Reference<UIText> textRef) {
        if (Reference.isInvalid(textRef)) {
            textRef = ensureValueTextRef(upgradeableStat);
        }
        setTextOnRef(textRef, (String.valueOf(upgradeableStat.getValue())));
    }

    /**
     * Sets text on the object if the reference is valid.
     *
     * @param textRef the reference to the {@link UIText}
     * @param value   the string to be set
     */
    private void setTextOnRef(Reference<UIText> textRef, String value) {
        if (Reference.isValid(textRef)) {
            textRef.get().setText(value);
        }
    }

    /**
     * Ensures the References For the cost text's exist.
     *
     * @param upgradeableStat the stat who's cost reference we are looking for
     * @return the reference assigned
     */
    private Reference<UIText> ensureCostTextRef(SyncStat upgradeableStat) {
        Reference<UIText> textReference = new Reference<UIText>(null);
        switch (upgradeableStat.getType()) {
            case ATTACK:
                textReference = mAttackCostText.getLabelText();
                if (mAttackLevelText != null) {
                    mTextCostReferences.put(StatType.ATTACK, textReference);
                }
                break;
            case DEFENCE:
                if (mDefenceLevelText != null) {
                    textReference = mDefenceCostText.getLabelText();
                    mTextCostReferences.put(StatType.DEFENCE, textReference);
                }
                break;
            case TOKEN_GENERATION:
                if (mTokenGenerationText != null) {
                    textReference = mTokenGenerationCostText.getLabelText();
                    mTextCostReferences.put(StatType.TOKEN_GENERATION, textReference);
                }
                break;
        }
        return textReference;
    }

    /**
     * Ensures the References For the value text's exist.
     *
     * @param upgradeableStat the stat who's value reference we are looking for
     * @return the reference assigned
     */
    private Reference<UIText> ensureValueTextRef(SyncStat upgradeableStat) {
        Reference<UIText> textReference = new Reference<UIText>(null);
        switch (upgradeableStat.getType()) {
            case ATTACK:
                textReference = mAttackLevelText.getLabelText();
                if (mAttackLevelText != null) {
                    mTextValueReferences.put(StatType.ATTACK, textReference);
                }
                break;
            case DEFENCE:
                if (mDefenceLevelText != null) {
                    textReference = mDefenceLevelText.getLabelText();
                    mTextValueReferences.put(StatType.DEFENCE, textReference);
                }
                break;
            case TOKEN_GENERATION:
                if (mTokenGenerationText != null) {
                    textReference = mTokenGenerationText.getLabelText();
                    mTextValueReferences.put(StatType.TOKEN_GENERATION, textReference);
                }
                break;
        }
        return textReference;
    }
}
