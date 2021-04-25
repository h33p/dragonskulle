/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

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
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.StandardHighlightType;
import org.dragonskulle.game.player.ui.Screen;
import org.dragonskulle.game.player.ui.UILinkedScrollBar;
import org.dragonskulle.game.player.ui.UIMenuLeftDrawer;
import org.dragonskulle.game.player.ui.UITokenCounter;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;
import org.joml.Vector3f;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class HumanPlayer extends Component implements IFrameUpdate, IFixedUpdate, IOnStart {

    // All screens to be used
    @Getter private Screen mScreenOn = Screen.DEFAULT_SCREEN;

    private Reference<UIMenuLeftDrawer> mMenuDrawer;

    // Data which is needed on different screens
    @Getter @Setter private HexagonTile mHexChosen;

    @Getter @Setter private Reference<Building> mBuildingChosen = new Reference<>(null);

    // The player
    private Reference<Player> mPlayer;
    private int mLocalTokens = 0;

    private final int mNetId;
    private final Reference<NetworkManager> mNetworkManager;

    // Visual effects
    private Reference<MapEffects> mMapEffects;
    private boolean mVisualsNeedUpdate;
    private Reference<UITokenCounter> mTokenCounter;
    private Reference<GameObject> mTokenCounterObject;
    private HexagonTile mLastHexChosen;

    private boolean mMovedCameraToCapital = false;

    /**
     * Create a {@link HumanPlayer}.
     *
     * @param networkManager The network manager.
     * @param netId The human player's network ID.
     */
    public HumanPlayer(Reference<NetworkManager> networkManager, int netId) {
        mNetworkManager = networkManager;
        mNetId = netId;
    }

    @Override
    public void onStart() {

        mMapEffects =
                Scene.getActiveScene()
                        .getSingleton(MapEffects.class)
                        .getReference(MapEffects.class);

        getGameObject()
                .buildChild(
                        "zoom_slider",
                        new TransformUI(true),
                        (go) -> go.addComponent(new UILinkedScrollBar()));

        Reference<GameObject> tmpRef =
                getGameObject()
                        .buildChild(
                                "menu_drawer",
                                new TransformUI(true),
                                (go) -> {
                                    mTokenCounterObject =
                                            go.buildChild(
                                                    "token_counter",
                                                    new TransformUI(true),
                                                    (self) -> {
                                                        UITokenCounter tokenCounter =
                                                                new UITokenCounter();
                                                        self.addComponent(tokenCounter);
                                                    });
                                    go.addComponent(
                                            new UIMenuLeftDrawer(
                                                    this::getBuildingChosen,
                                                    this::setBuildingChosen,
                                                    this::getHexChosen,
                                                    this::setHexChosen,
                                                    this::setScreenOn,
                                                    this::getPlayer));
                                });

        mTokenCounter = mTokenCounterObject.get().getComponent(UITokenCounter.class);
        mMenuDrawer = tmpRef.get().getComponent(UIMenuLeftDrawer.class);

        mVisualsNeedUpdate = true;
    }

    private Reference<Player> getPlayer() {
        return this.mPlayer;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        // Try getting the player if haven't already
        if (mPlayer == null) {
            NetworkManager manager = mNetworkManager.get();
            if (manager != null && manager.getClientManager() != null) {
                mPlayer =
                        manager.getClientManager()
                                .getNetworkObjects()
                                .filter(Reference::isValid)
                                .map(Reference::get)
                                .filter(NetworkObject::isMine)
                                .map(NetworkObject::getGameObject)
                                .map(go -> go.getComponent(Player.class))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
            }
        }

        if (!Reference.isValid(mPlayer)) {
            return;
        }

        if (!mMovedCameraToCapital) {
            TargetMovement targetRig = Scene.getActiveScene().getSingleton(TargetMovement.class);

            Building capital = mPlayer.get().getCapital();

            if (targetRig != null && capital != null) {
                targetRig.setTarget(capital.getGameObject().getTransform());
                mMovedCameraToCapital = true;
            }
        }

        if (mPlayer.get().hasLost()) {
            log.warning("You've lost your capital");
            setEnabled(false);
            return;
        }

        if (mPlayer.get().getNumberOfOwnedBuildings() == 0) {
            log.warning("You have 0 buildings -- should be sorted in mo");
            return;
        }
        // Update token
        if (Reference.isValid(mPlayer)) {
            updateVisibleTokens();
        }
    }

    private void updateVisibleTokens() {
        mLocalTokens = mPlayer.get().getTokens().get();
        if (Reference.isValid(mTokenCounter)) {
            mTokenCounter.get().setLabelReference(mLocalTokens);
        } else {
            mTokenCounter = getGameObject().getComponent(UITokenCounter.class);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        // Choose which screen to show

        if (Reference.isValid(mMenuDrawer)) {
            mMenuDrawer.get().setVisibleScreen(mScreenOn);
        }

        mapScreen();

        if (mVisualsNeedUpdate) {
            updateVisuals();
        }
    }

    /** This will choose what to do when the user can see the full map. */
    private void mapScreen() {

        Cursor cursor = Actions.getCursor();

        // Checks that its clicking something
        if (GameActions.LEFT_CLICK.isJustDeactivated()
                && (cursor == null || !cursor.hadLittleDrag())) {
            if (UIManager.getInstance().getHoveredObject() == null) {
                // And then select the tile
                Player player = mPlayer.get();
                if (player != null) {
                    HexagonMap component = player.getMap();
                    if (component != null) {
                        mLastHexChosen = mHexChosen;
                        mHexChosen = component.cursorToTile();
                        if (mHexChosen != null) {
                            Building building = mHexChosen.getBuilding();
                            if (building != null)
                                setBuildingChosen(building.getReference(Building.class));
                        }
                    }
                }

                if (mScreenOn == Screen.ATTACKING_SCREEN) {
                    return;
                }
                log.fine("Human:Got the Hexagon to enter");
                if (mHexChosen != null) {
                    if (mHexChosen.hasBuilding()) {
                        Building building = mHexChosen.getBuilding();

                        if (hasPlayerGotBuilding(building.getReference(Building.class))) {
                            mBuildingChosen = building.getReference(Building.class);
                            setScreenOn(Screen.BUILDING_SELECTED_SCREEN);
                        }
                    } else {
                        if (mHexChosen.isBuildable(player)) { // If you can build
                            // If you can build
                            log.info("Human:Change Screen");
                            setScreenOn(Screen.PLACING_NEW_BUILDING);
                        } else {
                            log.info("Human:Cannot build");
                            mBuildingChosen = null;
                            setScreenOn(Screen.DEFAULT_SCREEN);
                        }
                    }
                }
            } else if (GameActions.RIGHT_CLICK.isJustDeactivated()) {
                HexagonTile tile = mPlayer.get().getMap().cursorToTile();
                Vector3f pos = new Vector3f(tile.getQ(), tile.getR(), tile.getS());
                log.info("[DEBUG] RCL Position From Camera : " + pos);
            }
        }
    }

    /* AURI!! This updates what the user can see */
    private void updateVisuals() {
        if (mMapEffects == null || mPlayer == null) {
            return;
        }

        mVisualsNeedUpdate = false;

        Player player = mPlayer.get();

        if (player == null) {
            return;
        }

        MapEffects effects = mMapEffects.get();
        if (!mPlayer.get().hasLost()) {
            if (Reference.isValid(mMenuDrawer)) {
                mMenuDrawer.get().setVisibleScreen(mScreenOn);
            }

            effects.setActivePlayer(mPlayer);

            switch (mScreenOn) {
                case DEFAULT_SCREEN:
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(null);
                    break;
                case BUILDING_SELECTED_SCREEN:
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));
                    break;
                    //                case BUILD_TILE_SCREEN:
                    //                    effects.setDefaultHighlight(true);
                    //                    effects.setHighlightOverlay(
                    //                            (fx) -> highlightSelectedTile(fx,
                    // StandardHighlightType.PLAIN));
                    //                    break;
                case ATTACK_SCREEN:
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));
                    for (Building attackableBuilding :
                            mHexChosen.getBuilding().getAttackableBuildings()) {
                        effects.highlightTile(
                                attackableBuilding.getTile(),
                                StandardHighlightType.ATTACK.asSelection());
                    }
                    break;
                case UPGRADE_SCREEN:
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(null);
                    break;
                case ATTACKING_SCREEN:
                    break;
                case SELLING_SCREEN:
                    break;
                case PLACING_NEW_BUILDING:
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mScreenOn);
            }
        }
    }

    private void highlightSelectedTile(MapEffects fx, StandardHighlightType highlight) {
        if (mHexChosen != null) {
            fx.highlightTile(mHexChosen, highlight.asSelection());
        }
    }

    /**
     * Sets screen and notifies that an update is needed for the visuals.
     *
     * @param newScreen the new screen
     */
    public void setScreenOn(Screen newScreen) {
        if (!newScreen.equals(mScreenOn) || (mLastHexChosen != mHexChosen)) {
            mVisualsNeedUpdate = true;
        }
        mScreenOn = newScreen;
    }

    /**
     * A Method to check if the player owns that buildingSelectedView or not.
     *
     * @param buildingToCheck The buildingSelectedView to check
     * @return true if the player owns the buildingSelectedView, false if not
     */
    private boolean hasPlayerGotBuilding(Reference<Building> buildingToCheck) {
        if (!Reference.isValid(buildingToCheck)) {
            return false;
        }

        return mPlayer.get().getOwnedBuilding(buildingToCheck.get().getTile()) != null;
    }
}
