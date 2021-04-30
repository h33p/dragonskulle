/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.GameState.IGameEndEvent;
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
import org.dragonskulle.game.player.ui.UIPauseMenu;
import org.dragonskulle.game.player.ui.UITokenCounter;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
public class HumanPlayer extends Component implements IFrameUpdate, IFixedUpdate, IOnStart {

    /** The current {@link Screen} being displayed. */
    @Getter private Screen mCurrentScreen = Screen.DEFAULT_SCREEN;

    /** Stores the current HexagonTile selected. */
    @Getter @Setter private HexagonTile mHexChosen;
    /** Stores the current Building being selected, if there is one. */
    @Getter @Setter private Reference<Building> mBuildingChosen;

    /** Store a reference to the relevant {@link Player}. */
    private Reference<Player> mPlayer;

    /** Store a reference to the {@link NetworkManager}. */
    private final Reference<NetworkManager> mNetworkManager;

    /** Store a reference to the {@link MapEffects} component. */
    private Reference<MapEffects> mMapEffects;

    /** Store a reference to the {@link UIMenuLeftDrawer}. */
    @Getter private Reference<UIMenuLeftDrawer> mMenuDrawer;

    /** Store a reference to the {@link UITokenCounter} component. */
    private Reference<UITokenCounter> mTokenCounter;

    /** Store a reference to the {@link UILinkedScrollBar} component. */
    @Getter private Reference<UILinkedScrollBar> mScrollBar;

    /** Event that gets invoked whenever player loses its capital/the game ends. */
    private Reference<IGameEndEvent> mGameEndEventHandler;

    /** Whether the camera is moving to the capital. */
    private boolean mMovedCameraToCapital = false;

    /** Whether the visuals need to be updated. */
    private boolean mVisualsNeedUpdate = true;

    /**
     * Create a {@link HumanPlayer}.
     *
     * @param networkManager The network manager.
     */
    public HumanPlayer(Reference<NetworkManager> networkManager) {
        mNetworkManager = networkManager;
    }

    @Override
    public void onStart() {

        // Create the slider used for zooming.
        UILinkedScrollBar component = new UILinkedScrollBar();
        getGameObject()
                .buildChild(
                        "zoom_slider",
                        new TransformUI(true),
                        (go) -> {
                            go.addComponent(component);
                        });
        mScrollBar = component.getReference(UILinkedScrollBar.class);

        // Create the token counter.
        UITokenCounter counter = new UITokenCounter();
        mTokenCounter = counter.getReference(UITokenCounter.class);

        // Store the token counter in its own game object.
        GameObject tokenObject =
                new GameObject(
                        "token_counter",
                        new TransformUI(true),
                        (self) -> {
                            self.addComponent(counter);
                        });

        // Create the left menu.
        UIMenuLeftDrawer menu =
                new UIMenuLeftDrawer(
                        this::getBuildingChosen,
                        this::setBuildingChosen,
                        this::getHexChosen,
                        this::setHexChosen,
                        this::switchScreen,
                        this::getPlayer);
        mMenuDrawer = menu.getReference(UIMenuLeftDrawer.class);

        // Create a GameObject to store the token counter, and to hold the left menu.
        GameObject menuObject =
                new GameObject(
                        "menu_drawer",
                        new TransformUI(true),
                        (drawer) -> {
                            drawer.addChild(tokenObject);
                            drawer.addComponent(menu);
                        });
        getGameObject().addChild(menuObject);
    }

    @Override
    protected void onDestroy() {
        if (mGameEndEventHandler != null) {
            mGameEndEventHandler.clear();
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {

        Player player = getPlayer();
        if (player == null) return;

        // Initialise the event handler if haven't already.
        // This can lag by a frame or two, before game state gets spawned.
        if (!Reference.isValid(mGameEndEventHandler)) {
            GameState gameState = Scene.getActiveScene().getSingleton(GameState.class);

            Reference<UIPauseMenu> pauseMenu =
                    Scene.getActiveScene().getSingletonRef(UIPauseMenu.class);

            if (gameState != null && Reference.isValid(pauseMenu)) {
                mGameEndEventHandler =
                        new Reference<>(
                                (winnerId) -> {
                                    if (!Reference.isValid(pauseMenu)) {
                                        return;
                                    }

                                    if (Reference.isValid(mPlayer)) {
                                        int id = mPlayer.get().getNetworkObject().getOwnerId();

                                        String res = id == winnerId ? "You win!" : "You lose!";

                                        pauseMenu.get().endGame(res);
                                    }
                                });

                gameState.registerGameEndListener(mGameEndEventHandler);
            }
        }

        if (!mMovedCameraToCapital) {
            TargetMovement targetRig = Scene.getActiveScene().getSingleton(TargetMovement.class);
            Building capital = player.getCapital();

            if (targetRig != null && capital != null) {
                targetRig.setTarget(capital.getGameObject().getTransform());
                mMovedCameraToCapital = true;
            }
        }

        if (mPlayer.get().hasLost() && Reference.isValid(mGameEndEventHandler)) {
            mGameEndEventHandler.get().handle(-1000);
        }

        if (mPlayer.get().gameEnd()) {
            setEnabled(false);
            return;
        }

        // Update token
        updateVisibleTokens();
    }

    private void updateVisibleTokens() {
        Player player = getPlayer();
        if (player == null) return;

        if (Reference.isValid(mTokenCounter)) {
            mTokenCounter.get().setLabelReference(player.getTokens().get());
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        detectTileSelection();
        updateVisuals();
    }

    /**
     * Only allow the player to select opponent buildings.
     *
     * @param selected The hexagon tile clicked on.
     */
    private void attackSelect(HexagonTile selected) {
        Player player = getPlayer();
        if (!selected.hasBuilding()) return;
        Building building = selected.getBuilding();
        if (player.isBuildingOwner(building)) return;
        // mHexChosen = selected;
        mBuildingChosen = building.getReference(Building.class);
    }

    /** If the user selects a tile, swap to the relevant screen. */
    private void detectTileSelection() {
        Player player = getPlayer();
        Cursor cursor = Actions.getCursor();
        if (player == null || cursor == null) return;

        boolean click = GameActions.LEFT_CLICK.isJustDeactivated() && !cursor.hadLittleDrag();
        // If no clicking is occurring, exit.
        if (!click) return;

        // If the mouse is over the UI, exit.
        if (UIManager.getInstance().getHoveredObject() != null) return;

        // Ensure a tile can be selected.
        HexagonMap map = player.getMap();
        if (map == null) return;

        // Select a tile.
        HexagonTile selected = map.cursorToTile();
        if (selected == null) return;

        if (mCurrentScreen == Screen.ATTACKING_SCREEN) {
            attackSelect(selected);
            return;
        }

        mHexChosen = selected;

        // Do not swap screens.
        // if (mCurrentScreen == Screen.ATTACKING_SCREEN) return;

        if (mHexChosen.hasBuilding()) {
            Building building = mHexChosen.getBuilding();
            if (building == null) return;

            // Select the building.
            mBuildingChosen = building.getReference(Building.class);

            if (player.isBuildingOwner(building)) {
                switchScreen(Screen.BUILDING_SELECTED_SCREEN);
            } else {
                switchScreen(Screen.DEFAULT_SCREEN);
            }
        } else {
            if (mHexChosen.isBuildable(player)) {
                switchScreen(Screen.PLACING_NEW_BUILDING);
            } else {
                mBuildingChosen = null;
                switchScreen(Screen.DEFAULT_SCREEN);
            }
        }
    }

    /** This updates what the user can see */
    private void updateVisuals() {

        // Ensure Player and MapEffects exist.
        Player player = getPlayer();
        MapEffects effects = getMapEffects();
        if (player == null || effects == null) return;

        // Only run if visuals need updating.
        if (!mVisualsNeedUpdate) return;
        mVisualsNeedUpdate = false;

        if (player.hasLost()) return;

        // Set the player for the effects.
        effects.setActivePlayer(mPlayer);

        switch (mCurrentScreen) {
            case DEFAULT_SCREEN:
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightSelectedTile(fx, StandardHighlightType.VALID);
                        });
                break;
            case BUILDING_SELECTED_SCREEN:
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightSelectedTile(fx, StandardHighlightType.VALID);
                            highlightAttackableTiles(fx, StandardHighlightType.PLAIN);
                        });
                break;
            case ATTACKING_SCREEN:
                effects.setDefaultHighlight(true);
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightAttackableTiles(fx, StandardHighlightType.ATTACK);
                            highlightSelectedTile(fx, StandardHighlightType.VALID);
                        });
                break;
            case SELLING_SCREEN:
                break;
            case PLACING_NEW_BUILDING:
                effects.setDefaultHighlight(true);
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightBuildableTiles(fx, StandardHighlightType.VALID);
                            highlightSelectedTile(fx, StandardHighlightType.PLACE);
                        });
                break;
            default:
                break;
        }
    }

    /**
     * Switches to the specified {@link Screen} and notifies that an update is needed for the
     * visuals.
     *
     * @param desired The screen to switch to.
     */
    public void switchScreen(Screen desired) {
        if (desired.equals(mCurrentScreen)) return;

        if (Reference.isValid(mPlayer) && mPlayer.get().gameEnd()) {
            return;
        }

        mCurrentScreen = desired;

        if (Reference.isValid(mMenuDrawer)) {
            mMenuDrawer.get().setVisibleScreen(mCurrentScreen);
            mVisualsNeedUpdate = true;
        }
    }

    private void highlightSelectedTile(MapEffects effects, StandardHighlightType highlight) {
        if (mHexChosen == null || effects == null || highlight == null) return;
        effects.highlightTile(mHexChosen, highlight.asSelection());
    }

    private void highlightBuildableTiles(MapEffects fx, StandardHighlightType highlight) {
        Player player = getPlayer();
        if (player == null || fx == null || highlight == null) return;

        fx.highlightTiles(
                (tile, __) -> {
                    if (tile.isBuildable(player)) {
                        return highlight.asSelection();
                    }

                    // Do not highlight if the tile is already claimed, or if the tile is not
                    // visible.
                    if (tile.isClaimed() || !player.isTileViewable(tile)) {
                        return null;
                    }

                    if (tile.getTileType() != HexagonTile.TileType.FOG) {
                        return MapEffects.INVALID_MATERIAL;
                    }

                    return null;
                });
    }

    private void highlightAttackableTiles(MapEffects fx, StandardHighlightType highlight) {
        if (!Reference.isValid(mBuildingChosen) || fx == null || highlight == null) return;

        for (Building attackableBuilding : mBuildingChosen.get().getAttackableBuildings()) {
            fx.highlightTile(attackableBuilding.getTile(), highlight.asSelection());
        }
    }

    /**
     * Get the {@link Player} associated with this HumanPlayer.
     *
     * <p>If {@link #mPlayer} is not valid, it will attempt to get a valid Player.
     *
     * @return The {@link Player}, or {@code null} if there is no associated Player.
     */
    private Player getPlayer() {
        // Try getting the player if haven't already
        if (!Reference.isValid(mPlayer)) {
            if (!Reference.isValid(mNetworkManager)) return null;
            NetworkManager manager = mNetworkManager.get();

            if (manager == null || manager.getClientManager() == null) return null;
            Reference<Player> player =
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

            // Ensure player is not null (a valid Reference).
            if (!Reference.isValid(player)) return null;
            mPlayer = player;
        }

        return mPlayer.get();
    }

    /**
     * Get the {@link MapEffects}.
     *
     * <p>If {@link #mMapEffects} is not valid, it will attempt to get a valid MapEffects.
     *
     * @return The {@link MapEffects}; otherwise {@code null}.
     */
    private MapEffects getMapEffects() {
        if (!Reference.isValid(mMapEffects)) {
            MapEffects mapEffects = Scene.getActiveScene().getSingleton(MapEffects.class);

            if (mapEffects == null) return null;
            mMapEffects = mapEffects.getReference(MapEffects.class);
        }

        return mMapEffects.get();
    }
}
