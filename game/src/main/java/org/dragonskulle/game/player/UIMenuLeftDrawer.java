/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.network_data.BuildData;
import org.dragonskulle.game.player.network_data.SellData;
import org.dragonskulle.game.player.network_data.StatData;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;

/**
 * The menu drawer on the left side of the screen.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class UIMenuLeftDrawer extends Component implements IOnStart {
    private final IGetBuildingChosen mGetBuildingChosen;
    private final ISetBuildingChosen mSetBuildingChosen;
    private final IGetHexChosen mGetHexChosen;
    private final ISetHexChosen mSetHexChosen;
    private final INotifyScreenChange mNotifyScreenChange;
    private final IGetPlayer mGetPlayer;
    @Getter private HashMap<String, Reference<GameObject>> mButtonReferences = new HashMap<>();
    private final float mOffsetToTop = 0.46f;

    /** Notify the parent of the screen change and set it. */
    public interface INotifyScreenChange {
        /**
         * Call the function.
         *
         * @param newScreen the new screen
         */
        void call(Screen newScreen);
    }

    /** Get the player reference from the parent. */
    public interface IGetPlayer {
        /**
         * Get the player reference.
         *
         * @return the reference
         */
        Reference<Player> get();
    }

    /** Get the building chosen from the parent. */
    public interface IGetBuildingChosen {
        /**
         * Get the building.
         *
         * @return the reference
         */
        Reference<Building> get();
    }

    /** Get the hex chosen from the parent. */
    public interface IGetHexChosen {
        /**
         * Get the hexagon tile.
         *
         * @return the hexagon tile
         */
        HexagonTile get();
    }

    /** Set the parent hex tile. */
    public interface ISetHexChosen {
        /**
         * Set.
         *
         * @param tile the tile to set it to
         */
        void set(HexagonTile tile);
    }

    /** Set the building on the parent. */
    public interface ISetBuildingChosen {
        /**
         * Set the building.
         *
         * @param tile the tile
         */
        void set(Reference<Building> tile);
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
        ArrayList<UITextButtonFrame> menuButtons = new ArrayList<>();
        menuButtons.add(buildAttackButtonFrame());
        menuButtons.add(buildPlaceButtonFrame());
        menuButtons.add(buildUpgradeButtonFrame());
        menuButtons.add(buildSellButtonFrame());
        menuButtons.add(buildDeselectButtonFrame());

        mButtonReferences = buildMenu(menuButtons);

        UIRenderable drawer = new UIRenderable(GameUIAppearance.getDrawerTexture());
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setMargin(0f, 0f, 0f, 0f);
        tran.setPivotOffset(0f, 0f);
        tran.setParentAnchor(0f, 0f);
        getGameObject().addComponent(drawer);
    }

    /**
     * Build the attack button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildAttackButtonFrame() {
        return new UITextButtonFrame(
                "attack_button",
                "Attack From Here",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView
                    Reference<Building> buildingChosen = mGetBuildingChosen.get();
                    if (Reference.isValid(buildingChosen)) {

                        // TODO Change tiles which can be attacked
                        mSetHexChosen.set(null);
                        mNotifyScreenChange.call(Screen.ATTACKING_SCREEN);
                    } else {
                        mSetHexChosen.set(null);
                        mSetBuildingChosen.set(null);
                        mNotifyScreenChange.call(Screen.MAP_SCREEN);
                    }
                },
                false);
    }

    /**
     * Build the deselect button frame.
     *
     * @return the ui text button frame
     */
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

    /**
     * Build the place button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildPlaceButtonFrame() {
        return new UITextButtonFrame(
                "place_button",
                "Place Building",
                (handle, __) -> {
                    // -- Need way to show different buildingSelectedView
                    if (mGetHexChosen.get() != null) {
                        log.info("Running place button lambda");
                        Reference<Player> player = mGetPlayer.get();
                        if (Reference.isValid(player)) {
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

    /**
     * Build the upgrade button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildUpgradeButtonFrame() {
        return new UITextButtonFrame(
                "upgrade_button",
                "Upgrade Button",
                (handle, __) -> {
                    // TODO When clicked need to
                    // show options to upgrade
                    // buildingSelectedView stats.  Will leave
                    // until after prototype

                    // TODO Properly implement.

                    StatType statType = StatType.ATTACK;

                    Reference<Player> player = mGetPlayer.get();
                    if (Reference.isValid(player)) {
                        Reference<Building> buildingChosen = mGetBuildingChosen.get();
                        if (Reference.isValid(buildingChosen)) {
                            player.get()
                                    .getClientStatRequest()
                                    .invoke(
                                            new StatData(
                                                    buildingChosen.get(), statType)); // Send Data
                        }
                    }

                    mSetHexChosen.set(null);
                    mSetBuildingChosen.set(null);

                    mNotifyScreenChange.call(Screen.STAT_SCREEN);
                },
                false);
    }

    /**
     * Build the sell button frame.
     *
     * @return the ui text button frame
     */
    private UITextButtonFrame buildSellButtonFrame() {
        return new UITextButtonFrame(
                "sell_button",
                "Sell Building -- Not Done",
                (handle, __) -> {
                    // TODO When clicked need to
                    // sell buildingSelectedView

                    Reference<Player> player = mGetPlayer.get();
                    if (Reference.isValid(player)) {
                        Reference<Building> buildingChosen = mGetBuildingChosen.get();
                        if (Reference.isValid(buildingChosen)) {
                            player.get()
                                    .getClientSellRequest()
                                    .invoke(new SellData(buildingChosen.get())); // Send Data
                        }
                    }

                    mSetHexChosen.set(null);
                    mSetBuildingChosen.set(null);
                    mNotifyScreenChange.call(Screen.MAP_SCREEN);
                },
                false);
    }

    /**
     * Build the menu hash map.
     *
     * @param buttonChildren the button children
     * @return the hash map
     */
    private HashMap<String, Reference<GameObject>> buildMenu(
            List<UITextButtonFrame> buttonChildren) {
        HashMap<String, Reference<GameObject>> buttonMap = new HashMap<>();
        getGameObject()
                .buildChild(
                        "auto_built_children",
                        (menu) -> {
                            for (int i = 0, mButtonChildrenSize = buttonChildren.size();
                                    i < mButtonChildrenSize;
                                    i++) {
                                UITextButtonFrame mButtonChild = buttonChildren.get(i);
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
                                                                                    - mOffsetToTop);
                                                            self.getTransform(TransformUI.class)
                                                                    .setMargin(
                                                                            0.075f, 0f, -0.075f,
                                                                            0f);
                                                            UIButton button =
                                                                    new UIButton(
                                                                            mButtonChild.getText(),
                                                                            mButtonChild
                                                                                    .getOnClick(),
                                                                            mButtonChild
                                                                                    .isStartEnabled());
                                                            self.addComponent(button);
                                                        });

                                buttonMap.put(mButtonChild.getId(), button_reference);
                            }
                        });

        return buttonMap;
    }
}
