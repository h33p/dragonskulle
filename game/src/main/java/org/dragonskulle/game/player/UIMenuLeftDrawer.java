/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
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
@Accessors(prefix = "m")
public class UIMenuLeftDrawer extends Component implements IFrameUpdate, IOnStart {
    private final IGetBuildingChosen mGetBuildingChosen;
    private final ISetBuildingChosen mSetBuildingChosen;
    private final IGetHexChosen mGetHexChosen;
    private final ISetHexChosen mSetHexChosen;
    private final INotifyScreenChange mNotifyScreenChange;
    private final IGetPlayer mGetPlayer;
    private HashMap<String, Reference<GameObject>> mButtonReferences;
    private final float offsetToTop = 0.46f;

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
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        ArrayList<UITextButtonFrame> menuButtons = new ArrayList<>();
        menuButtons.add(buildAttackButtonFrame());
        menuButtons.add(buildPlaceButtonFrame());
        menuButtons.add(buildUpgradeButtonFrame());
        menuButtons.add(buildSellButtonFrame());
        menuButtons.add(buildDeselectButtonFrame());

        mButtonReferences = buildMenu(menuButtons);

        UIRenderable drawer = new UIRenderable(new SampledTexture("ui/drawer.png"));
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setMargin(0f, 0f, 0f, 0f);
        tran.setPosition(-1.56f, 0f);
        tran.setParentAnchor(0, 0);
        getGameObject().addComponent(drawer);
    }

    private UITextButtonFrame buildAttackButtonFrame() {
        return new UITextButtonFrame(
                "attack_button",
                "Attack buildingSelectedView",
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
                });
    }

    private UITextButtonFrame buildDeselectButtonFrame() {
        return new UITextButtonFrame(
                "deselect_button",
                "Deselect Tile",
                (handle, __) -> {
                    mSetHexChosen.set(null);
                    mSetBuildingChosen.set(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                });
    }

    private UITextButtonFrame buildPlaceButtonFrame() {
        return new UITextButtonFrame(
                "place_button",
                "Place Building",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView
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
                });
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
                    mNotifyScreenChange.call(Screen.STAT_SCREEN);
                });
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
                });
    }

    private HashMap<String, Reference<GameObject>> buildMenu(
            List<UITextButtonFrame> mButtonChildren) {
        HashMap<String, Reference<GameObject>> buttonMap = new HashMap<>();
        getGameObject()
                .buildChild(
                        "auto_built_children",
                        (menu) -> {
                            for (int i = 0, mButtonChildrenSize = mButtonChildren.size();
                                    i < mButtonChildrenSize;
                                    i++) {
                                UITextButtonFrame mButtonChild = mButtonChildren.get(i);
                                int finalI = i;
                                Reference<GameObject> button_reference =
                                        getGameObject()
                                                .buildChild(
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
                                                                                    - offsetToTop);
                                                            self.getTransform(TransformUI.class)
                                                                    .setMargin(
                                                                            0.075f, 0f, -0.075f,
                                                                            0f);
                                                            self.addComponent(
                                                                    new UIRenderable(
                                                                            new SampledTexture(
                                                                                    "ui/wide_button_new.png")));
                                                            UIButton button =
                                                                    new UIButton(
                                                                            new UIText(
                                                                                    new Vector3f(
                                                                                            0f, 0f,
                                                                                            0f),
                                                                                    Font
                                                                                            .getFontResource(
                                                                                                    "Rise of Kingdom.ttf"),
                                                                                    mButtonChild
                                                                                            .getText()),
                                                                            mButtonChild
                                                                                    .getOnClick());
                                                            self.addComponent(button);
                                                        });

                                buttonMap.put(mButtonChild.getId(), button_reference);
                            }
                        });

        return buttonMap;
    }

    public void setMenu(Screen mScreenOn) {
        Reference<GameObject> button;
        switch (mScreenOn) {
            case BUILDING_SELECTED_SCREEN:
                button = mButtonReferences.get("select_button");
                if (button != null && button.isValid()) {
                    // should disable button
                }
                break;
            case TILE_SCREEN:
                button = mButtonReferences.get("select_button");
                if (button != null && button.isValid()) {
                    // should disable button
                }
                break;
            case ATTACK_SCREEN:
                button = mButtonReferences.get("attack_button");
                if (button != null && button.isValid()) {
                    // should disable button
                }
                break;
            case STAT_SCREEN:
                button = mButtonReferences.get("stats_button");
                if (button != null && button.isValid()) {
                    // should disable button
                }
                break;
        }
    }
}
