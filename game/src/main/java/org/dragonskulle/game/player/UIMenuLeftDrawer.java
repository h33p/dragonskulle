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
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

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

    private final float mOffsetToTop = 0.46f;
    private final ArrayList<UITextButtonFrame> mAdditionalItems = new ArrayList<>();
    @Getter private Reference<UIShopSection> mShop;
    private Reference<GameObject> mBuildScreenMenu;
    private Reference<GameObject> mAttackScreenMenu;
    private Reference<GameObject> mStatScreenMenu;
    private Reference<GameObject> mMapScreenMenu;
    private Reference<GameObject> mTileSelectedMenu;

    @Setter @Getter private Reference<GameObject> mCurrentScreen = new Reference<>(null);

    public interface INotifyScreenChange {
        void call(Screen newScreen);
    }

    public interface IGetPlayer {
        Reference<Player> get();
    }

    public interface IGetBuildingChosen {
        Reference<Building> get();
    }

    public interface IGetHexChosen {
        HexagonTile get();
    }

    public interface ISetHexChosen {
        void set(HexagonTile tile);
    }

    public interface ISetBuildingChosen {
        void set(Reference<Building> tile);
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
        ArrayList<UITextButtonFrame> buildingSelectedScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> statScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> mapScreenMenuItems = new ArrayList<>();
        ArrayList<UITextButtonFrame> tileSelectedScreenMenuItems = new ArrayList<>();

        attackScreenMenuItems.add(buildAttackButtonFrame());
        attackScreenMenuItems.add(buildDeselectButtonFrame());
        mAttackScreenMenu = buildMenu(attackScreenMenuItems);

        buildingSelectedScreenMenuItems.add(buildAttackButtonFrame());
        buildingSelectedScreenMenuItems.add(buildSellButtonFrame());
        buildingSelectedScreenMenuItems.add(buildUpgradeButtonFrame());
        buildingSelectedScreenMenuItems.add(buildDeselectButtonFrame());
        mBuildScreenMenu = buildMenu(buildingSelectedScreenMenuItems);

        tileSelectedScreenMenuItems.add(buildPlaceButtonFrame());
        tileSelectedScreenMenuItems.add(buildDeselectButtonFrame());
        mTileSelectedMenu = buildMenu(tileSelectedScreenMenuItems);

        statScreenMenuItems.add(buildDeselectButtonFrame());
        mStatScreenMenu = buildMenu(statScreenMenuItems);

        mapScreenMenuItems.add(buildPlaceButtonFrame());
        mapScreenMenuItems.add(buildDeselectButtonFrame());
        mMapScreenMenu = buildMenu(mapScreenMenuItems);

        setVisibleScreen(Screen.MAP_SCREEN);
        mShop = buildShop();

        UIRenderable drawer = new UIRenderable(new SampledTexture("ui/drawer.png"));
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setMargin(0f, 0f, 0f, 0f);
        tran.setPivotOffset(0f, 0f);
        tran.setParentAnchor(0f, 0f);
        getGameObject().addComponent(drawer);
    }

    public void setVisibleScreen(Screen screen) {
        Reference<GameObject> newScreen;
        switch (screen) {
            case MAP_SCREEN:
                newScreen = mMapScreenMenu;
                setShopState(UIShopSection.ShopState.CLOSED);
                break;
            case BUILDING_SELECTED_SCREEN:
                newScreen = mBuildScreenMenu;
                setShopState(UIShopSection.ShopState.BUILDING_SELECTED);
                break;
            case BUILD_TILE_SCREEN:
                newScreen = mTileSelectedMenu;
                setShopState(UIShopSection.ShopState.BUILDING_NEW);
                break;
            case ATTACK_SCREEN:
                newScreen = mAttackScreenMenu;
                setShopState(UIShopSection.ShopState.CLOSED);
                break;
            case UPGRADE_SCREEN:
                newScreen = mStatScreenMenu;
                setShopState(UIShopSection.ShopState.UPGRADE);
                break;
            default:
                log.warning("Menu hasn't been updated to reflect this screen yet");
                newScreen = mMapScreenMenu;
                setShopState(UIShopSection.ShopState.CLOSED);
        }
        if (mCurrentScreen.isValid()) {
            mCurrentScreen.get().setEnabled(false);
        }
        mCurrentScreen = newScreen;
        mCurrentScreen.get().setEnabled(true);
    }

    private void setShopState(UIShopSection.ShopState shopState) {
        if (mShop != null && getShop().isValid()) {
            getShop().get().setState(shopState);
        }
    }

    private UITextButtonFrame buildAttackButtonFrame() {
        return new UITextButtonFrame(
                "attack_button",
                "Attack Selected Building",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView

                    // Send attackView to server
                    //            mPlayer.get()
                    //                    .getClientAttackRequest()
                    //                    .invoke(
                    //                            new AttackData(
                    //                                    mBuildingChosen.get(),
                    //                                    attackableBuilding));
                    // for (Building attackableBuilding :
                    // mBuildingChosen.get().getAttackableBuildings()) {
                    //                attackBuildingsButton.add(
                    //                        new UITextButtonFrame("Attack buildingSelectedView",
                    // (handle, __) -> {
                    //                            // -- Need way to show different
                    // buildingSelectedView
                    //
                    //                            // Send attackView to server
                    //                            mPlayer.get()
                    //                                    .getClientAttackRequest()
                    //                                    .invoke(
                    //                                            new AttackData(
                    //                                                    mBuildingChosen.get(),
                    //                                                    attackableBuilding)); //
                    // TODO Send
                    //                            setHexChosen.set(null);
                    //                            mBuildingChosen = null;
                    //                            notifyScreenChange.call(Screen.MAP_SCREEN);
                    //                        })
                    //                );
                    //            }
                    //
                    //
                    //
                    // TODO change so this button will display the attackable buildings, then if the
                    // user clicks on one it will show a prompt to confirm.
                    mSetHexChosen.set(null);
                    mSetBuildingChosen.set(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                true);
    }

    private UITextButtonFrame buildDeselectButtonFrame() {
        return new UITextButtonFrame(
                "deselect_button",
                "Deselect Tile",
                (handle, __) -> {
                    mSetHexChosen.set(null);
                    mSetBuildingChosen.set(null);
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
                    if (mGetHexChosen.get() != null) {
                        System.out.println("Running place button lambda");
                        Reference<Player> player = mGetPlayer.get();
                        if (player != null && player.isValid()) {
                            player.get()
                                    .getClientBuildRequest()
                                    .invoke(new BuildData(mGetHexChosen.get()));
                        }

                        mSetHexChosen.set(null);
                        mSetBuildingChosen.set(null);
                        mNotifyScreenChange.call(Screen.MAP_SCREEN);
                    }
                },
                true);
    }

    private UITextButtonFrame buildUpgradeButtonFrame() {
        return new UITextButtonFrame(
                "upgrade_button",
                "Upgrade Button",
                (handle, __) -> {
                    // TODO When clicked need to
                    // show options to upgrade
                    // buildingSelectedView stats.  Will leave
                    // until after prototype
                    mNotifyScreenChange.call(Screen.UPGRADE_SCREEN);
                },
                true);
    }

    private UITextButtonFrame buildSellButtonFrame() {
        return new UITextButtonFrame(
                "sell_button",
                "Sell Building -- Not Done",
                (handle, __) -> {
                    // TODO When clicked need to
                    // sell buildingSelectedView

                    Reference<Player> player = mGetPlayer.get();
                    if (player != null && player.isValid()) {
                        Reference<Building> buildingChosen = mGetBuildingChosen.get();
                        if (buildingChosen != null && buildingChosen.isValid()) {
                            player.get()
                                    .getClientSellRequest()
                                    .invoke(new SellData(buildingChosen.get())); // Send Data
                        }
                    }

                    mSetHexChosen.set(null);
                    mSetBuildingChosen.set(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                true);
    }

    /**
     * Build a menu, it is disabled by default.
     *
     * @param mButtonChildren the buttons to be built
     * @return reference to the built menu.
     */
    private Reference<GameObject> buildMenu(List<UITextButtonFrame> mButtonChildren) {
        mButtonChildren.addAll(this.mAdditionalItems);

        Reference<GameObject> ref =
                getGameObject()
                        .buildChild(
                                "built_menu",
                                new TransformUI(),
                                (root) -> {
                                    for (int i = 0, mButtonChildrenSize = mButtonChildren.size();
                                            i < mButtonChildrenSize;
                                            i++) {
                                        UITextButtonFrame mButtonChild = mButtonChildren.get(i);
                                        int finalI = i;
                                        root.buildChild(
                                                "drawer_child_" + i,
                                                new TransformUI(true),
                                                (self) -> {
                                                    self.getTransform(TransformUI.class)
                                                            .setPosition(
                                                                    0f,
                                                                    (0.8f
                                                                                    * finalI
                                                                                    / mButtonChildrenSize
                                                                                    * 1.3f)
                                                                            - mOffsetToTop);
                                                    self.getTransform(TransformUI.class)
                                                            .setMargin(0.075f, 0f, -0.075f, 0f);
                                                    self.addComponent(
                                                            new UIRenderable(
                                                                    new SampledTexture(
                                                                            "ui/wide_button_new.png")));
                                                    UIButton button =
                                                            new UIButton(
                                                                    new UIText(
                                                                            new Vector3f(
                                                                                    0f, 0f, 0f),
                                                                            Font.getFontResource(
                                                                                    "Rise of Kingdom.ttf"),
                                                                            mButtonChild.getText()),
                                                                    mButtonChild.getOnClick(),
                                                                    mButtonChild.isStartEnabled());
                                                    self.addComponent(button);
                                                });
                                    }
                                });
        ref.get().setEnabled(false);
        return ref;
    }

    private Reference<UIShopSection> buildShop() {
        getGameObject()
                .buildChild(
                        "shop", new TransformUI(), (go) -> go.addComponent(new UIShopSection()));
        ArrayList<Reference<UIShopSection>> shops = new ArrayList<>();
        getGameObject().getComponentsInChildren(UIShopSection.class, shops);
        if (shops.size() != 0) {
            mShop = shops.get(0);
        }
        return mShop;
    }
}
