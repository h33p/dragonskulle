/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.UIShopSection.ShopState;
import org.dragonskulle.game.player.network_data.AttackData;
import org.dragonskulle.game.player.network_data.BuildData;
import org.dragonskulle.game.player.network_data.SellData;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;

/** @author Oscar L */
@Log
@Accessors(prefix = "m")
public class UIMenuLeftDrawer extends Component implements IOnStart {
    private final IGetBuildingChosen mGetBuildingChosen;
    private final ISetBuildingChosen mSetBuildingChosen;
    private final IGetHexChosen mGetHexChosen;
    private final ISetHexChosen mSetHexChosen;
    private final INotifyScreenChange mNotifyScreenChange;
    private final IGetPlayer mGetPlayer;

    private final float mOffsetToTop = 0.25f;
    @Getter private Reference<UIShopSection> mShop;
    private Reference<GameObject> mBuildScreenMenu;
    private Reference<GameObject> mAttackScreenMenu;
    private Reference<GameObject> mMapScreenMenu;
    private Reference<GameObject> mTileSelectedMenu;
    private Reference<GameObject> mSellConfirmScreenMenu;

    @Setter @Getter private Reference<GameObject> mCurrentScreen = new Reference<>(null);
    @Setter @Getter private Screen mLastScreen = null;

    public interface INotifyScreenChange {
        void call(Screen newScreen);
    }

    public interface IGetPlayer {
        Reference<Player> getPlayer();
    }

    public interface IGetBuildingChosen {
        Reference<Building> getBuilding();
    }

    public interface IGetHexChosen {
        HexagonTile getHex();
    }

    public interface ISetHexChosen {
        void setHex(HexagonTile tile);
    }

    public interface ISetBuildingChosen {
        void setBuilding(Reference<Building> tile);
    }

    public UIMenuLeftDrawer(
            IGetBuildingChosen getBuildingChosen,
            ISetBuildingChosen setBuildingChosen,
            IGetHexChosen getHexChosen,
            ISetHexChosen setHexChosen,
            INotifyScreenChange notifyScreenChange,
            IGetPlayer mGetPlayer) {
        super();
        this.mGetBuildingChosen = getBuildingChosen;
        this.mSetBuildingChosen = setBuildingChosen;
        this.mGetHexChosen = getHexChosen;
        this.mSetHexChosen = setHexChosen;
        this.mNotifyScreenChange = notifyScreenChange;
        this.mGetPlayer = mGetPlayer;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        ArrayList<UITextButtonFrame> attackScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> mSellConfirmScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> buildingSelectedScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> mapScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> tileSelectedScreenMenuItems = new ArrayList<>();

        attackScreenMenuItems.add(buildConfirmAttackButtonFrame());
        attackScreenMenuItems.add(buildCancelAttackButtonFrame());
        mAttackScreenMenu = buildMenu(attackScreenMenuItems);

        mSellConfirmScreenMenuItems.add(buildConfirmSellButtonFrame());
        mSellConfirmScreenMenuItems.add(buildCancelSellButtonFrame());
        mSellConfirmScreenMenu = buildMenu(mSellConfirmScreenMenuItems);

        buildingSelectedScreenMenuItems.add(buildAttackButtonFrame());
        buildingSelectedScreenMenuItems.add(buildSellButtonFrame());
        buildingSelectedScreenMenuItems.add(buildDeselectButtonFrame());
        mBuildScreenMenu = buildMenu(buildingSelectedScreenMenuItems);

        tileSelectedScreenMenuItems.add(buildPlaceButtonFrame());
        tileSelectedScreenMenuItems.add(buildDeselectButtonFrame());
        mTileSelectedMenu = buildMenu(tileSelectedScreenMenuItems);

        mapScreenMenuItems.add(buildPlaceButtonFrame());
        mapScreenMenuItems.add(buildDeselectButtonFrame());
        mMapScreenMenu = buildMenu(mapScreenMenuItems);

        setVisibleScreen(Screen.MAP_SCREEN);
        mShop = buildShop();

        UIRenderable drawer = new UIRenderable(GameUIAppearance.getDrawerTexture());
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setMargin(0f, 0f, 0f, 0f);
        tran.setPivotOffset(0f, 0f);
        tran.setParentAnchor(0f, 0f);
        getGameObject().addComponent(drawer);
    }

    private UITextButtonFrame buildCancelAttackButtonFrame() {
        return new UITextButtonFrame(
                "cancel_attack",
                "Cancel Attack",
                (handle, __) -> {
                    mSetHexChosen.setHex(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                true);
    }

    private UITextButtonFrame buildCancelSellButtonFrame() {
        return new UITextButtonFrame(
                "cancel_sell",
                "Cancel Sell",
                (handle, __) -> {
                    mSetHexChosen.setHex(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                true);
    }

    private UITextButtonFrame buildConfirmAttackButtonFrame() {
        return new UITextButtonFrame(
                "confirm_attack_button",
                "Attack Selected",
                (handle, __) -> {
                    Reference<Building> attackingBuilding = mGetBuildingChosen.getBuilding();
                    Building defendingBuilding = mGetHexChosen.getHex().getBuilding();

                    if (Reference.isValid(attackingBuilding) && defendingBuilding != null) {
                        // Checks the building can be attacked
                        boolean canAttack =
                                attackingBuilding.get().isBuildingAttackable(defendingBuilding);
                        if (canAttack) {
                            Reference<Player> playerReference = mGetPlayer.getPlayer();
                            if (Reference.isValid(playerReference)) {
                                playerReference
                                        .get()
                                        .getClientAttackRequest()
                                        .invoke(
                                                new AttackData(
                                                        attackingBuilding.get(),
                                                        defendingBuilding));
                            }

                            mNotifyScreenChange.call(Screen.MAP_SCREEN);
                            mSetHexChosen.setHex(null);
                        }
                    }
                },
                true);
    }

    private UITextButtonFrame buildConfirmSellButtonFrame() {
        return new UITextButtonFrame(
                "confirm_sell_button",
                "Sell Selected for " + Building.SELL_PRICE,
                (handle, __) -> {
                    Reference<Building> buildingToSell = mGetBuildingChosen.getBuilding();
                    if (Reference.isValid(buildingToSell)) {
                        Reference<Player> player = mGetPlayer.getPlayer();
                        if (Reference.isValid(player)) {
                            player.get()
                                    .getClientSellRequest()
                                    .invoke(new SellData(buildingToSell.get())); // Send Data
                        }
                    }
                    mSetHexChosen.setHex(null);
                    mSetBuildingChosen.setBuilding(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                true);
    }

    public void setVisibleScreen(Screen screen) {
        Reference<GameObject> newScreen = new Reference<>(null);
        if (screen != getLastScreen()) {
            log.warning("setting visible screen to " + screen.toString());
            switch (screen) {
                case UPGRADE_SCREEN:
                case BUILDING_SELECTED_SCREEN:
                    newScreen = mBuildScreenMenu;
                    setShopState(ShopState.MY_BUILDING_SELECTED);
                    break;
                case BUILD_TILE_SCREEN:
                    newScreen = mTileSelectedMenu;
                    setShopState(ShopState.CLOSED);
                    //                    final HexagonTile tile = mGetHexChosen.get();
                    //                    if (tile != null &&
                    // tile.isBuildable(mGetPlayer.get().get())) {
                    //                        setShopState(ShopState.BUILDING_NEW);
                    //                    } TODO we can add this functionality if needed
                    break;
                case ATTACK_SCREEN:
                    newScreen = mAttackScreenMenu;
                    setShopState(ShopState.CLOSED);
                    break;
                case ATTACKING_SCREEN:
                    newScreen = mAttackScreenMenu;
                    setShopState(ShopState.CLOSED);
                    break;
                case SELLING_SCREEN:
                    newScreen = mSellConfirmScreenMenu;
                    setShopState(ShopState.CLOSED);
                    break;
                default:
                    log.warning("Menu hasn't been updated to reflect this screen yet");
                    newScreen = mMapScreenMenu;
                    setShopState(ShopState.CLOSED);
            }
            setLastScreen(screen);
            swapScreens(newScreen);
        }
    }

    private void swapScreens(Reference<GameObject> newScreen) {
        if (Reference.isValid(mCurrentScreen)) {
            // there is a screen being shown
            // deactivate the panel
            show(mCurrentScreen, false);
        }
        // activate the new panel and assign the last current variable
        mCurrentScreen = activateNewScreen(newScreen);
    }

    private void show(boolean showShow) {
        getGameObject().setEnabled(showShow);
    }

    private void show(Reference<GameObject> gameObject, boolean show) {
        gameObject.get().setEnabled(show);
    }

    private Reference<GameObject> activateNewScreen(Reference<GameObject> newScreen) {
        // check if valid reference then reassign
        if (Reference.isValid(newScreen)) {
            show(newScreen, true);
            return newScreen;
        }
        return new Reference<>(null);
    }

    private void setShopState(ShopState shopState) {
        if (Reference.isValid(mShop)) {
            getShop().get().setState(shopState);
        }
    }

    private UITextButtonFrame buildAttackButtonFrame() {
        return new UITextButtonFrame(
                "attack_button",
                "Attack From Here",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView
                    Reference<Building> buildingChosen = mGetBuildingChosen.getBuilding();
                    if (Reference.isValid(buildingChosen)) {
                        mSetHexChosen.setHex(null);
                        mNotifyScreenChange.call(Screen.ATTACKING_SCREEN);
                    } else {
                        mSetHexChosen.setHex(null);
                        mSetBuildingChosen.setBuilding(null);
                        mNotifyScreenChange.call(Screen.MAP_SCREEN);
                    }
                },
                true);
    }

    private UITextButtonFrame buildDeselectButtonFrame() {
        return new UITextButtonFrame(
                "deselect_button",
                "Deselect Tile",
                (handle, __) -> {
                    mSetHexChosen.setHex(null);
                    mSetBuildingChosen.setBuilding(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                true);
    }

    private UITextButtonFrame buildPlaceButtonFrame() {
        return new UITextButtonFrame(
                "place_button",
                "Place Building",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView
                    if (mGetHexChosen.getHex() != null) {
                        log.info("Running place button lambda");
                        Reference<Player> player = mGetPlayer.getPlayer();
                        if (Reference.isValid(player)) {
                            player.get()
                                    .getClientBuildRequest()
                                    .invoke(new BuildData(mGetHexChosen.getHex()));
                        }

                        mSetHexChosen.setHex(null);
                        mSetBuildingChosen.setBuilding(null);
                        mNotifyScreenChange.call(Screen.MAP_SCREEN);
                    }
                },
                true);
    }

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

        final GameObject built_menu = new GameObject("build_menu", new TransformUI());
        manager.buildVerticalUI(built_menu, mOffsetToTop, 0f, 1f, buttons);
        getGameObject().addChild(built_menu);
        built_menu.setEnabled(false);
        return built_menu.getReference();
    }

    private Reference<UIShopSection> buildShop() {
        getGameObject()
                .buildChild(
                        "shop",
                        false,
                        new TransformUI(),
                        (go) -> go.addComponent(new UIShopSection(mGetPlayer, mGetHexChosen)));
        ArrayList<Reference<UIShopSection>> shops = new ArrayList<>();
        getGameObject().getComponentsInChildren(UIShopSection.class, shops);
        if (shops.size() != 0) {
            mShop = shops.get(0);
        }
        return mShop;
    }
}
