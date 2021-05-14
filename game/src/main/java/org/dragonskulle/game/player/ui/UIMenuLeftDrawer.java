/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.ArrayList;
import java.util.List;
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
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.network_data.SellData;
import org.dragonskulle.game.player.ui.UIShopSection.ShopState;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;
import org.dragonskulle.utils.TextUtils;

/**
 * The menu drawer on the left side of the screen.
 *
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class UIMenuLeftDrawer extends Component implements IOnStart, IFixedUpdate {
    protected final IGetBuildingChosen mGetBuildingChosen;
    protected final ISetBuildingChosen mSetBuildingChosen;
    protected final IGetHexChosen mGetHexChosen;
    protected final ISetHexChosen mSetHexChosen;
    protected final INotifyScreenChange mNotifyScreenChange;
    protected final IGetPlayer mGetPlayer;
    protected final IUpdateBuildingChosen mUpdateBuildingSelected;
    @Getter protected final ISetPredefinedBuildingChosen mSetPredefinedBuildingChosen;
    private final float mOffsetToTop = 0.25f;
    @Getter private Reference<UIShopSection> mShop;
    private Reference<GameObject> mBuildScreenMenu;
    private Reference<GameObject> mAttackScreenMenu;
    private Reference<GameObject> mSellConfirmScreenMenu;

    @Setter @Getter private Reference<GameObject> mCurrentScreen = new Reference<>(null);
    @Setter @Getter private Screen mLastScreen = null;

    @Setter
    @Accessors(fluent = true, prefix = "m")
    private boolean mIsHidden = false;

    private TransformUI mTransform;
    private Reference<UITextRect> mAttackConfirm;
    private Reference<UITextRect> mAttackCost;
    private Reference<UITextRect> mAttackInfo;
    private Reference<UIButton> mAttackOption;
    private Reference<UIButton> mSellOption;
    private Reference<UIButton> mSellButton;

    /**
     * Hides the menu drawer.
     *
     * @param hide true if the drawer should hide, false if it should show
     */
    public void setHidden(boolean hide) {
        if (hide && mIsHidden) return;
        if (!hide && !mIsHidden) return;
        if (hide) {
            mTransform.translate(-100f, 0);
            isHidden(true);
        } else {
            mTransform.translate(100f, 0);
            isHidden(false);
        }
    }

    /** Notify the parent of the screen change and set it. */
    public interface INotifyScreenChange {
        /**
         * Call the function.
         *
         * @param newScreen the new screen
         */
        void call(Screen newScreen);
    }

    /** Update the parents building field. */
    public interface IUpdateBuildingChosen {
        /** Call the function. */
        void update();
    }

    /** Get the {@link Player} from the parent. */
    public interface IGetPlayer {
        /**
         * Get the player.
         *
         * @return the reference
         */
        Player getPlayer();
    }

    /** Get the building chosen from the parent. */
    public interface IGetBuildingChosen {
        /**
         * Get the building.
         *
         * @return the reference
         */
        Reference<Building> getBuilding();
    }

    /** Get the hex chosen from the parent. */
    public interface IGetHexChosen {
        /**
         * Get the hexagon tile.
         *
         * @return the hexagon tile
         */
        HexagonTile getHex();
    }

    /** Set the parent hex tile. */
    public interface ISetHexChosen {
        /**
         * Set.
         *
         * @param tile the tile to set it to
         */
        void setHex(HexagonTile tile);
    }

    /** Set the building on the parent. */
    public interface ISetBuildingChosen {
        /**
         * Set the building.
         *
         * @param tile the tile
         */
        void setBuilding(Reference<Building> tile);
    }

    /** Set the predefined building used on the parent. */
    public interface ISetPredefinedBuildingChosen {
        /**
         * Set the predefined building.
         *
         * @param descriptor the descriptor
         */
        void setPredefinedBuilding(BuildingDescriptor descriptor);
    }

    /**
     * Constructor.
     *
     * @param getBuildingChosen the get building chosen callback
     * @param setBuildingChosen the set building chosen callback
     * @param getHexChosen the get hex chosen callback
     * @param setHexChosen the set hex chosen callback
     * @param notifyScreenChange the notify screen change callback
     * @param getPlayer the get player callback
     * @param setPredefinedBuildingChosen the set predefined building callback
     */
    public UIMenuLeftDrawer(
            IGetBuildingChosen getBuildingChosen,
            ISetBuildingChosen setBuildingChosen,
            IGetHexChosen getHexChosen,
            ISetHexChosen setHexChosen,
            INotifyScreenChange notifyScreenChange,
            IGetPlayer getPlayer,
            ISetPredefinedBuildingChosen setPredefinedBuildingChosen) {
        super();
        this.mGetBuildingChosen = getBuildingChosen;
        this.mSetBuildingChosen = setBuildingChosen;
        this.mGetHexChosen = getHexChosen;
        this.mSetHexChosen = setHexChosen;
        this.mNotifyScreenChange = notifyScreenChange;
        this.mGetPlayer = getPlayer;
        this.mSetPredefinedBuildingChosen = setPredefinedBuildingChosen;

        this.mUpdateBuildingSelected =
                () -> {
                    if (mGetHexChosen == null
                            || mSetHexChosen == null
                            || mGetBuildingChosen == null
                            || mSetBuildingChosen == null) return;

                    if (mLastScreen == Screen.ATTACKING_SCREEN) return;

                    HexagonTile tile = mGetHexChosen.getHex();
                    if (tile == null) return;
                    Building building = tile.getBuilding();
                    if (building == null) return;
                    mSetBuildingChosen.setBuilding(building.getReference(Building.class));
                };
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
        // The menu that displays when you select a building.
        generateBuildingSelectedMenu();
        // The menu that displays when you enter attack mode.
        generateAttackMenu();
        // The sell confirmation menu.
        generateSellMenu();

        setVisibleScreen(Screen.DEFAULT_SCREEN);
        mShop = buildShop();

        UIRenderable drawer = new UIRenderable(GameUIAppearance.getDrawerTexture());
        mTransform = getGameObject().getTransform(TransformUI.class);
        mTransform.setMargin(0f, 0f, 0f, 0f);
        mTransform.setPivotOffset(0f, 0f);
        mTransform.setParentAnchor(0f, 0f);
        getGameObject().addComponent(drawer);
    }

    /** Generate the attack menu components and store the menu in {@link #mAttackScreenMenu}. */
    private void generateAttackMenu() {
        ArrayList<UITextButtonFrame> items = new ArrayList<>();

        // The attack menu.
        items.add(buildCancelAttackButtonFrame());
        mAttackScreenMenu = buildMenu(items, 0.25f);

        if (!Reference.isValid(mAttackScreenMenu)) return;
        GameObject menu = mAttackScreenMenu.get();

        if (menu.getChildren().size() == 0) return;
        // Move the cancel button down slightly.
        GameObject cancelButton = menu.getChildren().get(0);
        cancelButton.getTransform(TransformUI.class).translate(0, 0.2f);

        // Add an attack cost label.
        UITextRect cost = new UITextRect("Cost: ?");
        mAttackCost = cost.getReference(UITextRect.class);

        GameObject costObject =
                new GameObject(
                        "attack_cost",
                        new TransformUI(),
                        (object) -> {
                            object.addComponent(cost);
                        });
        TransformUI costTransform = costObject.getTransform(TransformUI.class);
        costTransform.setParentAnchor(0.02f, 0.07f, 1f - 0.02f, 0.07f + 0.08f);
        menu.addChild(costObject);

        // Add an attack info label.
        UITextRect attackInfo = new UITextRect(TextUtils.constructField("Chance", "---", 15));
        mAttackInfo = attackInfo.getReference(UITextRect.class);

        GameObject attackInfoObject =
                new GameObject(
                        "attack_info",
                        new TransformUI(),
                        (object) -> {
                            object.addComponent(attackInfo);
                        });
        TransformUI infoTransform = attackInfoObject.getTransform(TransformUI.class);
        infoTransform.setParentAnchor(0.02f, 0.155f, 1f - 0.02f, 0.155f + 0.08f);
        menu.addChild(attackInfoObject);

        // Add an attack confirm label.
        UITextRect attackConfirm = new UITextRect("Attack Selected");
        mAttackConfirm = attackConfirm.getReference(UITextRect.class);

        GameObject attackConfirmObject =
                new GameObject(
                        "attack_confirm",
                        new TransformUI(),
                        (object) -> {
                            object.addComponent(attackConfirm);
                        });
        TransformUI attackConfirmTransform = attackConfirmObject.getTransform(TransformUI.class);
        attackConfirmTransform.setParentAnchor(0.02f, 0.24f, 1f - 0.02f, 0.24f + 0.08f);
        menu.addChild(attackConfirmObject);
    }

    /** Generate the sell menu components and store the menu in {@link #mSellConfirmScreenMenu}. */
    private void generateSellMenu() {
        ArrayList<UITextButtonFrame> items = new ArrayList<>();

        items.add(buildConfirmSellButtonFrame());
        items.add(buildCancelSellButtonFrame());
        mSellConfirmScreenMenu = buildMenu(items, 0.15f);

        if (!Reference.isValid(mSellConfirmScreenMenu)) return;
        GameObject menu = mSellConfirmScreenMenu.get();

        ArrayList<GameObject> children = menu.getChildren();
        if (menu.getChildren().size() == 0) return;
        mSellButton = children.get(0).getComponent(UIButton.class);
    }

    /**
     * Generate the building selected menu components and store the menu in {@link
     * #mBuildScreenMenu}.
     */
    private void generateBuildingSelectedMenu() {
        ArrayList<UITextButtonFrame> items = new ArrayList<>();

        items.add(buildAttackButtonFrame());
        items.add(buildSellButtonFrame());
        mBuildScreenMenu = buildMenu(items);

        if (!Reference.isValid(mBuildScreenMenu)) return;
        GameObject menu = mBuildScreenMenu.get();

        ArrayList<GameObject> children = menu.getChildren();
        if (menu.getChildren().size() == 0) return;
        mAttackOption = children.get(0).getComponent(UIButton.class);
        mSellOption = children.get(1).getComponent(UIButton.class);

        TransformUI transform = menu.getTransform(TransformUI.class);
        if (transform != null) {
            transform.translate(0f, 0.55f);
        }
    }

    /**
     * Build the attack button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildCancelAttackButtonFrame() {
        return new UITextButtonFrame(
                "cancel_attack",
                "Cancel Attack",
                (handle, __) -> {
                    // Reset the Building back to the HexagonTile's building.
                    mUpdateBuildingSelected.update();

                    mNotifyScreenChange.call(Screen.BUILDING_SELECTED_SCREEN);
                },
                true);
    }

    /**
     * Build the cancel sell button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildCancelSellButtonFrame() {
        return new UITextButtonFrame(
                "cancel_sell",
                "Cancel Sell",
                (handle, __) -> {
                    mNotifyScreenChange.call(Screen.BUILDING_SELECTED_SCREEN);
                },
                true);
    }

    /**
     * Build the confirm sell button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildConfirmSellButtonFrame() {
        return new UITextButtonFrame(
                "confirm_sell_button",
                "Sell Building",
                (handle, __) -> {
                    Reference<Building> buildingToSell = mGetBuildingChosen.getBuilding();
                    if (Reference.isValid(buildingToSell)) {
                        Building building = buildingToSell.get();

                        Player player = mGetPlayer.getPlayer();
                        if (player != null) {
                            player.getClientSellRequest()
                                    .invoke(new SellData(building)); // Send Data
                        }
                    }
                    mSetHexChosen.setHex(null);
                    mSetBuildingChosen.setBuilding(null);
                    mNotifyScreenChange.call(Screen.DEFAULT_SCREEN);
                },
                true);
    }

    /**
     * Sets the visible screen to the game.
     *
     * @param screen the screen to be shown
     */
    public void setVisibleScreen(Screen screen) {
        Reference<GameObject> newScreen = new Reference<>(null);
        if (screen != getLastScreen()) {
            log.fine("setting visible screen to " + screen.toString());
            switch (screen) {
                case DEFAULT_SCREEN:
                    newScreen = null;
                    setShopState(ShopState.DEFAULT);
                    break;
                case BUILDING_SELECTED_SCREEN:
                    newScreen = mBuildScreenMenu;
                    setShopState(ShopState.MY_BUILDING_SELECTED, true);
                    break;
                case ATTACKING_SCREEN:
                    newScreen = mAttackScreenMenu;
                    setShopState(ShopState.CLOSED);
                    break;
                case SELLING_SCREEN:
                    newScreen = mSellConfirmScreenMenu;
                    setShopState(ShopState.CLOSED);

                    if (!Reference.isValid(mSellButton)) break;
                    if (!Reference.isValid(mGetBuildingChosen.getBuilding())) break;

                    Reference<UIText> text = mSellButton.get().getLabelText();
                    if (!Reference.isValid(text)) break;
                    text.get()
                            .setText(
                                    "Sell for "
                                            + mGetBuildingChosen
                                                    .getBuilding()
                                                    .get()
                                                    .getSellPrice());

                    break;
                case PLACING_NEW_BUILDING:
                    newScreen = null;
                    setShopState(ShopState.BUILDING_NEW);
                    break;
                default:
                    log.fine("Menu hasn't been updated to reflect this screen yet");
                    newScreen = null;
                    setShopState(ShopState.CLOSED);
            }
            setLastScreen(screen);
            swapScreens(newScreen);
        }
    }

    /**
     * Swap the menu screens.
     *
     * @param newScreen the new screen to be shown
     */
    private void swapScreens(Reference<GameObject> newScreen) {
        if (Reference.isValid(mCurrentScreen)) {
            // there is a screen being shown
            // deactivate the panel
            show(mCurrentScreen, false);
        }
        // activate the new panel and assign the last current variable
        mCurrentScreen = activateNewScreen(newScreen);
    }

    /**
     * Show the game object from reference.
     *
     * @param gameObject the game object
     * @param show true to show, false to hide
     */
    private void show(Reference<GameObject> gameObject, boolean show) {
        gameObject.get().setEnabled(show);
    }

    /**
     * Activate the new screen by reference. This shows the new screen to the game.
     *
     * @param newScreen the new screen
     * @return the new activated screen, a null reference if the new screen was null.
     */
    private Reference<GameObject> activateNewScreen(Reference<GameObject> newScreen) {
        // check if valid reference then reassign
        if (Reference.isValid(newScreen)) {
            show(newScreen, true);
            return newScreen;
        }
        return new Reference<>(null);
    }

    /**
     * Sets the shop state.
     *
     * @param shopState the new state
     */
    private void setShopState(ShopState shopState) {
        if (Reference.isValid(mShop)) {
            getShop().get().setState(shopState);
        }
    }

    /**
     * Sets the shop state.
     *
     * @param shopState the new state
     * @param updateBuilding this will force the parents mSelectedBuilding variable to update
     */
    private void setShopState(ShopState shopState, boolean updateBuilding) {
        if (updateBuilding) mUpdateBuildingSelected.update();
        setShopState(shopState);
    }

    /**
     * Build the attack button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildAttackButtonFrame() {
        return new UITextButtonFrame(
                "attack_button",
                "Launch Attack!",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView
                    Reference<Building> buildingChosen = mGetBuildingChosen.getBuilding();
                    if (Reference.isValid(buildingChosen)) {
                        mNotifyScreenChange.call(Screen.ATTACKING_SCREEN);
                    } else {
                        mSetBuildingChosen.setBuilding(null);
                        mNotifyScreenChange.call(Screen.DEFAULT_SCREEN);
                    }
                },
                true);
    }

    /**
     * Build the sell button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildSellButtonFrame() {
        return new UITextButtonFrame(
                "sell_button",
                "Sell Building",
                (handle, __) -> mNotifyScreenChange.call(Screen.SELLING_SCREEN),
                true);
    }

    /**
     * Build a menu, it is disabled by default.
     *
     * @param mButtonChildren the buttons to be built
     * @param yOffset How much to offset the y position by.
     * @return reference to the built menu.
     */
    private Reference<GameObject> buildMenu(
            List<UITextButtonFrame> mButtonChildren, float yOffset) {
        UIManager manager = UIManager.getInstance();
        UIButton[] buttons =
                mButtonChildren.stream()
                        .map(
                                child ->
                                        new UIButton(
                                                child.getText(),
                                                child.getOnClick(),
                                                child.isStartEnabled()))
                        .toArray(UIButton[]::new);

        final GameObject menu = new GameObject("build_menu", new TransformUI());
        manager.buildVerticalUi(menu, mOffsetToTop, 0f, 1f, buttons);
        getGameObject().addChild(menu);

        TransformUI transform = menu.getTransform(TransformUI.class);
        if (transform != null) {
            transform.setParentAnchor(0.1f, -0.1f + yOffset, 1f - 0.1f, 1.2f + yOffset);
        }

        menu.setEnabled(false);
        return menu.getReference();
    }

    /**
     * Build a menu of {@link UITextButtonFrame} components.
     *
     * @param mButtonChildren the menu children
     * @return the reference to the built menu
     */
    private Reference<GameObject> buildMenu(List<UITextButtonFrame> mButtonChildren) {
        return buildMenu(mButtonChildren, 0f);
    }

    /**
     * Build the shop component.
     *
     * @return the reference to the shop
     */
    private Reference<UIShopSection> buildShop() {
        getGameObject()
                .buildChild(
                        "shop",
                        true,
                        new TransformUI(),
                        (go) -> go.addComponent(new UIShopSection(this)));
        ArrayList<Reference<UIShopSection>> shops = new ArrayList<>();
        getGameObject().getComponentsInChildren(UIShopSection.class, shops);
        if (shops.size() != 0) {
            mShop = shops.get(0);
        }
        return mShop;
    }

    /**
     * Ensure the chosen HexagonTile has a player owned building. If it does not, go to the default
     * screen and close the shop.
     */
    private void checkOwnership() {
        HexagonTile tile = mGetHexChosen.getHex();
        if (tile == null || !tile.hasBuilding()) return;
        Building building = tile.getBuilding();

        Player player = mGetPlayer.getPlayer();
        if (player == null) return;

        if (!player.isBuildingOwner(building)) {
            setShopState(ShopState.CLOSED, true);
            mNotifyScreenChange.call(Screen.DEFAULT_SCREEN);
        }
    }

    /** Display is selling the building is possible. */
    private void updateSellOptionButton() {

        if (mLastScreen != Screen.BUILDING_SELECTED_SCREEN) return;

        if (!Reference.isValid(mSellOption)) return;

        Reference<Building> buildingRef = mGetBuildingChosen.getBuilding();
        if (!Reference.isValid(buildingRef)) return;
        Building building = buildingRef.get();

        Reference<UIText> sellText = mSellOption.get().getLabelText();
        if (!Reference.isValid(sellText)) return;

        if (building.isCapital()) {
            sellText.get().setText("[CANNOT SELL CAPITAL]");
            mSellOption.get().setEnabled(false);
        } else {
            sellText.get().setText("Sell Building");
            mSellOption.get().setEnabled(true);
        }
    }

    /** Display if an attack is possible. */
    private void updateAttackOptionButton() {

        if (mLastScreen != Screen.BUILDING_SELECTED_SCREEN) return;

        if (!Reference.isValid(mAttackOption)) return;
        UIButton option = mAttackOption.get();

        HexagonTile tile = mGetHexChosen.getHex();
        if (tile == null || !tile.hasBuilding()) return;
        Building building = tile.getBuilding();

        Player player = mGetPlayer.getPlayer();
        if (player == null) return;

        Reference<UIText> text = option.getLabelText();
        if (!Reference.isValid(text)) return;

        if (building.getAttackableBuildings().size() == 0) {
            text.get().setText("[OUT OF RANGE]");
            option.setEnabled(false);
            return;
        }

        int cost = building.getAttackCost();
        if (cost <= player.getTokens().get()) {
            text.get().setText("Launch Attack");
            option.setEnabled(true);
        } else {
            text.get().setText(String.format("[TOO EXPENSIVE: %d]", cost));
            option.setEnabled(false);
        }
    }

    /** Display the cost. */
    private void updateAttackCostText() {
        if (mLastScreen != Screen.ATTACKING_SCREEN) return;

        if (!Reference.isValid(mAttackCost)) return;
        UITextRect textRect = mAttackCost.get();

        HexagonTile tile = mGetHexChosen.getHex();
        if (tile == null || !tile.hasBuilding()) return;
        Building attacker = tile.getBuilding();

        Reference<UIText> text = textRect.getLabelText();
        if (!Reference.isValid(text)) return;

        int cost = attacker.getAttackCost();
        text.get().setText("Cost: " + cost);
    }

    /** Display the chance of success. */
    private void updateAttackInfoText() {
        if (mLastScreen != Screen.ATTACKING_SCREEN) return;

        if (!Reference.isValid(mAttackInfo)) return;
        UITextRect textRect = mAttackInfo.get();

        HexagonTile tile = mGetHexChosen.getHex();
        if (tile == null || !tile.hasBuilding()) return;
        Building attacker = tile.getBuilding();

        Reference<Building> defenderReference = mGetBuildingChosen.getBuilding();
        if (!Reference.isValid(defenderReference)) return;
        Building defender = defenderReference.get();

        Player player = mGetPlayer.getPlayer();
        if (player == null) return;

        Reference<UIText> text = textRect.getLabelText();
        if (!Reference.isValid(text)) return;

        if (player.isBuildingOwner(defender)) {
            text.get().setText(TextUtils.constructField("Chance", "---", 15));
            return;
        }

        String output = String.format("%.0f%%", attacker.calculateAttackOdds(defender) * 100);

        text.get().setText(TextUtils.constructField("Chance", output, 15));
    }

    /** Display if the player can attack or is in a cooldown. */
    private void updateAttackConfirmText() {
        if (mLastScreen != Screen.ATTACKING_SCREEN) return;

        if (!Reference.isValid(mAttackConfirm)) return;
        UITextRect textRect = mAttackConfirm.get();

        Player player = mGetPlayer.getPlayer();
        if (player == null) return;

        Reference<UIText> text = textRect.getLabelText();
        if (!Reference.isValid(text)) return;

        HexagonTile tile = mGetHexChosen.getHex();
        if (tile == null || !tile.hasBuilding()) return;
        Building attacker = tile.getBuilding();

        if (attacker == null || attacker.getOwner() != player) return;

        if (player.inCooldown() || attacker.isActionLocked()) {

            float cooldown = player.getRemainingCooldown();

            cooldown =
                    Math.max(
                            cooldown,
                            attacker.getActionLockTime().get()
                                    - player.getNetworkManager().getServerTime());

            text.get().setText(String.format("[COOLDOWN: %.1f]", cooldown));
            return;
        }

        int cost = attacker.getAttackCost();

        if (cost > player.getTokens().get()) {
            text.get().setText("[TOO EXPENSIVE]");
            return;
        }

        text.get().setText("[SELECT BUILDING]");

        Reference<Building> defenderReference = mGetBuildingChosen.getBuilding();
        if (!Reference.isValid(defenderReference)) return;
        Building defender = defenderReference.get();

        if (player.isBuildingOwner(defender)) {
            return;
        }

        if (defender.isActionLocked()) {
            float cooldown =
                    defender.getActionLockTime().get() - player.getNetworkManager().getServerTime();

            text.get().setText(String.format("[IN ATTACK: %.1f]", cooldown));
            return;
        }

        text.get().setText("Attack!");
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        checkOwnership();
        updateSellOptionButton();
        updateAttackOptionButton();
        updateAttackCostText();
        updateAttackInfoText();
        updateAttackConfirmText();
    }
}
