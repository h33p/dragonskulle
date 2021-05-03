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
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.network_data.AttackData;
import org.dragonskulle.game.player.network_data.SellData;
import org.dragonskulle.game.player.ui.UIShopSection.ShopState;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

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
    private Reference<UIButton> mAttackConfirm;
    private Reference<UITextRect> mAttackCost;
    private Reference<UITextRect> mAttackInfo;
    private Reference<UIButton> mAttackOption;
    private Reference<UIButton> mSellOption;
    private Reference<UIButton> mSellButton;

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

    /**
     * Constructor.
     *
     * @param getBuildingChosen the get building chosen callback
     * @param setBuildingChosen the set building chosen callback
     * @param getHexChosen the get hex chosen callback
     * @param setHexChosen the set hex chosen callback
     * @param notifyScreenChange the notify screen change callback
     * @param getPlayer the get player callback
     */
    public UIMenuLeftDrawer(
            IGetBuildingChosen getBuildingChosen,
            ISetBuildingChosen setBuildingChosen,
            IGetHexChosen getHexChosen,
            ISetHexChosen setHexChosen,
            INotifyScreenChange notifyScreenChange,
            IGetPlayer getPlayer) {
        super();
        this.mGetBuildingChosen = getBuildingChosen;
        this.mSetBuildingChosen = setBuildingChosen;
        this.mGetHexChosen = getHexChosen;
        this.mSetHexChosen = setHexChosen;
        this.mNotifyScreenChange = notifyScreenChange;
        this.mGetPlayer = getPlayer;

        this.mUpdateBuildingSelected =
                () -> {
                    if (mGetHexChosen == null
                            || mSetHexChosen == null
                            || mGetBuildingChosen == null
                            || mSetBuildingChosen == null) return;

                    HexagonTile tile = mGetHexChosen.getHex();
                    if (tile == null) return;
                    Building building = tile.getBuilding();
                    if (building == null) return;
                    mSetBuildingChosen.setBuilding(building.getReference(Building.class));

                    System.out.println("run!");
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

    private void generateAttackMenu() {
        ArrayList<UITextButtonFrame> items = new ArrayList<>();

        // The attack menu.
        items.add(buildConfirmAttackButtonFrame());
        items.add(buildCancelAttackButtonFrame());
        mAttackScreenMenu = buildMenu(items);

        if (!Reference.isValid(mAttackScreenMenu)) return;
        GameObject menu = mAttackScreenMenu.get();

        ArrayList<GameObject> children = menu.getChildren();
        if (menu.getChildren().size() == 0) return;
        mAttackConfirm = children.get(0).getComponent(UIButton.class);

        // Move all the buttons down.
        for (GameObject object : menu.getChildren()) {
            TransformUI objectTransform = object.getTransform(TransformUI.class);
            objectTransform.translate(0, 0.4f);
        }

        // Move the 2nd button up slightly.
        if (children.size() >= 1) {
            GameObject secondButton = menu.getChildren().get(1);
            secondButton.getTransform(TransformUI.class).translate(0, -0.05f);
        }

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
        costTransform.setParentAnchor(0.02f, 0.225f, 1f - 0.02f, 0.225f + 0.08f);
        menu.addChild(costObject);

        // Add an attack info label.
        UITextRect attackInfo = new UITextRect("Chance: ------");
        mAttackInfo = attackInfo.getReference(UITextRect.class);

        GameObject attackInfoObject =
                new GameObject(
                        "attack_info",
                        new TransformUI(),
                        (object) -> {
                            object.addComponent(attackInfo);
                        });
        TransformUI infoTransform = attackInfoObject.getTransform(TransformUI.class);
        infoTransform.setParentAnchor(0.02f, 0.315f, 1f - 0.02f, 0.315f + 0.08f);
        menu.addChild(attackInfoObject);
    }

    private void generateSellMenu() {
        ArrayList<UITextButtonFrame> items = new ArrayList<>();

        items.add(buildConfirmSellButtonFrame());
        items.add(buildCancelSellButtonFrame());
        mSellConfirmScreenMenu = buildMenu(items);

        if (!Reference.isValid(mSellConfirmScreenMenu)) return;
        GameObject menu = mSellConfirmScreenMenu.get();

        ArrayList<GameObject> children = menu.getChildren();
        if (menu.getChildren().size() == 0) return;
        mSellButton = children.get(0).getComponent(UIButton.class);
    }

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
            transform.translate(0f, 0.7f);
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

                    setShopState(ShopState.CLOSED, true);
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
                	setShopState(ShopState.CLOSED, true);
                	mNotifyScreenChange.call(Screen.BUILDING_SELECTED_SCREEN);
                },
                true);
    }

    /**
     * Build the confirm attack button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildConfirmAttackButtonFrame() {
        return new UITextButtonFrame(
                "confirm_attack_button",
                "Attack Selected",
                (handle, __) -> {
                    HexagonTile tile = mGetHexChosen.getHex();
                    if (tile == null || !tile.hasBuilding()) return;
                    Building attacking = tile.getBuilding();

                    Reference<Building> defendingRef = mGetBuildingChosen.getBuilding();
                    if (!Reference.isValid(defendingRef)) return;
                    Building defending = defendingRef.get();

                    // Checks the building can be attacked
                    if (!attacking.isBuildingAttackable(defending)) return;

                    Player player = mGetPlayer.getPlayer();
                    if (player == null) return;
                    player.getClientAttackRequest().invoke(new AttackData(attacking, defending));

                    // Reset the Building back to the HexagonTile's building.
                    mUpdateBuildingSelected.update();

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
            log.warning("setting visible screen to " + screen.toString());
            switch (screen) {
                case DEFAULT_SCREEN:
                    newScreen = null;
                    setShopState(ShopState.CLOSED);
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
                    log.warning("Menu hasn't been updated to reflect this screen yet");
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
     * @return reference to the built menu.
     */
    private Reference<GameObject> buildMenu(List<UITextButtonFrame> mButtonChildren) {
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
            transform.setParentAnchor(0.1f, -0.1f, 1f - 0.1f, 1f);
        }

        menu.setEnabled(false);
        return menu.getReference();
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
     * Ensure the chosen HexagonTile has a player owned building. If it does not, go to the default screen and close the shop.
     */
    private void checkOwnership() {

        // if (mLastScreen != Screen.ATTACKING_SCREEN) return;

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

    private void updateAttackOptionButton() {

        if (mLastScreen != Screen.BUILDING_SELECTED_SCREEN) return;

        if (!Reference.isValid(mAttackOption)) return;
        UIButton option = mAttackOption.get();

        Reference<Building> buildingRef = mGetBuildingChosen.getBuilding();
        if (!Reference.isValid(buildingRef)) return;
        Building building = buildingRef.get();

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
            text.get().setText("Launch Attack for " + cost);
            option.setEnabled(true);
        } else {
            text.get().setText(String.format("[TOO EXPENSIVE: %d]", cost));
            option.setEnabled(false);
        }
    }

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
            text.get().setText("Chance: ------");
            return;
        }

        int attackLevel = attacker.getAttack().getLevel();
        int defendLevel = defender.getDefence().getLevel();
        int difference = attackLevel - defendLevel;
        String output = "MEDIUM";

        if (difference <= -5) {
            output = "VERY LOW";
        } else if (difference <= -3) {
            output = "LOW";
        }

        if (difference >= 5) {
            output = "VERY HIGH";
        } else if (difference >= 3) {
            output = "HIGH";
        }

        text.get().setText(String.format("Chance: %s", output));
    }

    private void updateAttackConfirmButton() {
        if (mLastScreen != Screen.ATTACKING_SCREEN) return;

        if (!Reference.isValid(mAttackConfirm)) return;
        UIButton button = mAttackConfirm.get();

        Reference<Building> defenderReference = mGetBuildingChosen.getBuilding();
        if (!Reference.isValid(defenderReference)) return;
        Building defender = defenderReference.get();

        HexagonTile tile = mGetHexChosen.getHex();
        if (tile == null || !tile.hasBuilding()) return;
        Building attacker = tile.getBuilding();

        Reference<UIText> text = button.getLabelText();
        if (!Reference.isValid(text)) return;

        Player player = mGetPlayer.getPlayer();
        if (player == null) return;

        if (player.inCooldown()) {
            text.get()
                    .setText(
                            String.format(
                                    "[COOLDOWN: %d]", (int) player.getRemainingCooldown() + 1));
            button.setEnabled(false);
            return;
        }

        if (player.isBuildingOwner(defender)) {
            text.get().setText("[SELECT BUILDING]");
            button.setEnabled(false);
            return;
        }

        int cost = attacker.getAttackCost();
        if (cost <= player.getTokens().get()) {
            text.get().setText("Attack!");
            button.setEnabled(true);
        } else {
            text.get().setText("[TOO EXPENSIVE]");
            button.setEnabled(false);
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        checkOwnership();
        updateSellOptionButton();
        updateAttackOptionButton();
        updateAttackCostText();
        updateAttackInfoText();
        updateAttackConfirmButton();
    }
}
