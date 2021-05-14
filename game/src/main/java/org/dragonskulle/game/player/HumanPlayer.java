/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.App;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.GameState.IGameEndEvent;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.StandardHighlightType;
import org.dragonskulle.game.misc.ArcPath;
import org.dragonskulle.game.misc.ArcPath.IArcHandler;
import org.dragonskulle.game.misc.ArcPath.IPathUpdater;
import org.dragonskulle.game.player.network_data.BuildData;
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
import org.dragonskulle.utils.MathUtils;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;

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

    /** Predefined building descriptor to use for placing buildings. */
    @Getter @Setter
    private BuildingDescriptor mPredefinedBuildingChosen = PredefinedBuildings.get(0);

    /** Store a reference to the relevant {@link Player}. */
    private Reference<Player> mPlayer;

    /** Store a reference to the {@link NetworkManager}. */
    private final Reference<NetworkManager> mNetworkManager;

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

    /** Whether the game is paused and hover highlights should be disabled. */
    private boolean mIsPaused = false;

    /** Visual arc when aiming at a building to attack. */
    private class ArcUpdater implements IPathUpdater, IArcHandler {

        private final Vector3f mPosStart = new Vector3f();
        private final Vector3f mPosEnd = new Vector3f();

        private float mLerpedStart = -1;
        private boolean mDidSet = false;

        @Override
        public void handle(ArcPath arcPath) {
            float speed = 10f;
            float deltaTime = Engine.getInstance().getFrameDeltaTime();
            float lerptime = Math.min(1f, deltaTime * speed);

            Building target = Reference.isValid(mBuildingChosen) ? mBuildingChosen.get() : null;
            HexagonTile start = mHexChosen;
            Player player = mPlayer.get();

            if (mArcFadeOut
                    || target == null
                    || start == null
                    || target.getTile() == null
                    || !player.attackCheck(start.getBuilding(), target)) {
                mLerpedStart = MathUtils.lerp(mLerpedStart, -1, lerptime);

                arcPath.setSpawnOffset(mLerpedStart);

                if (mLerpedStart <= -0.95f) {
                    mDidSet = false;

                    if (mArcFadeOut) {
                        mUpdater.clear();
                    }
                }

                return;
            } else {
                mLerpedStart = MathUtils.lerp(mLerpedStart, 0, lerptime);
            }

            arcPath.setSpawnOffset(mLerpedStart);

            HexagonTile hex = start;
            HexagonTile hexEnd = target.getTile();

            TransformHex.axialToCartesian(
                    new Vector2f(hex.getQ(), hex.getR()), hex.getHeight(), mPosStart);
            TransformHex.axialToCartesian(
                    new Vector2f(hexEnd.getQ(), hexEnd.getR()), hexEnd.getSurfaceHeight(), mPosEnd);

            HexagonMap map = mPlayer.get().getMap();

            Matrix4fc mat = map.getGameObject().getTransform().getWorldMatrix();

            mat.transformPosition(mPosStart);
            mat.transformPosition(mPosEnd);

            float amplitude = hex.distTo(hexEnd.getQ(), hexEnd.getR()) * 0.2f + 0.5f;

            if (!mDidSet) {
                mArcPath.get().getPosStart().set(mPosStart);
                mArcPath.get().getPosTarget().set(mPosEnd);

                mArcPath.get().setAmplitude(amplitude);

                mDidSet = true;
            }

            arcPath.getPosStart().lerp(mPosStart, lerptime);
            arcPath.getPosTarget().lerp(mPosEnd, lerptime);
            arcPath.setAmplitude(MathUtils.lerp(arcPath.getAmplitude(), amplitude, lerptime));
        }

        @Override
        public void handle(int id, float pathPoint, Transform3D transform) {
            float factor = 1.6f;

            float mul = factor * pathPoint - 0.5f * factor;

            mul = -(mul * mul) + 1;

            transform.setScale(mul, mul, mul);
        }
    }

    /** Visual arc path shown on selections. */
    private Reference<ArcPath> mArcPath;

    /** Updater for mArcPath. */
    private Reference<ArcUpdater> mUpdater;

    private boolean mArcFadeOut = false;

    static final GameObject SELECTION_TEMPLATE =
            App.TEMPLATES.get().getDefaultScene().findRootObject("selection_sphere");

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
                        this::getPlayer,
                        this::setPredefinedBuildingChosen);
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

        ArcPath path = new ArcPath();

        path.setTemplate(SELECTION_TEMPLATE);
        path.setAmplitude(2f);
        path.setObjGap(0.05f);

        getGameObject().addComponent(path);

        mArcPath = path.getReference(ArcPath.class);
        // this shouldnt have to happen to play on start
        AudioSource src = new AudioSource();
        getGameObject().addComponent(src);
        src.playSound(GameUIAppearance.AudioFiles.ON_GAME_START.getPath());
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
                                        boolean didWin = id == winnerId;
                                        pauseMenu.get().endGame(didWin);
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

    /** Update visible tokens on UI. */
    private void updateVisibleTokens() {
        Player player = getPlayer();
        if (player == null) return;

        if (Reference.isValid(mTokenCounter)) {
            mTokenCounter.get().setVisibleTokens(player.getTokens().get());
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {

        UIPauseMenu pauseMenu = Scene.getActiveScene().getSingleton(UIPauseMenu.class);

        mIsPaused = pauseMenu != null && pauseMenu.isEnabled() && pauseMenu.isPaused();

        if (mIsPaused) {
            return;
        }

        detectTileSelection();
        detectBackAction();
        detectKeyboardInput();
        updateVisuals();
    }

    /** Detect keyboard input for switching actions. */
    private void detectKeyboardInput() {
        Screen nextScreen = mCurrentScreen;

        switch (mCurrentScreen) {
            case BUILDING_SELECTED_SCREEN:
                if (Reference.isValid(mBuildingChosen)) {
                    Building b = mBuildingChosen.get();
                    if (!b.getAttackableBuildings().isEmpty()
                            && GameActions.ATTACK_MODE.isJustActivated()) {
                        nextScreen = Screen.ATTACKING_SCREEN;
                    }

                    if (!b.isCapital() && GameActions.SELL_MODE.isJustActivated()) {
                        nextScreen = Screen.SELLING_SCREEN;
                    }
                }
                // fallthrough
            case SELLING_SCREEN:
            case ATTACKING_SCREEN:
            case DEFAULT_SCREEN:
                if (GameActions.BUILD_MODE.isJustActivated()) {
                    nextScreen = Screen.PLACING_NEW_BUILDING;
                }
                break;
            default:
                break;
        }

        if (mCurrentScreen != nextScreen) {
            switchScreen(nextScreen);
        }
    }

    /** Detect when the player wants to move back. */
    private void detectBackAction() {
        Player player = getPlayer();
        Cursor cursor = Actions.getCursor();
        if (player == null || cursor == null) return;

        boolean click = GameActions.RIGHT_CLICK.isJustDeactivated() && !cursor.hadLittleDrag();
        // If no clicking is occurring, exit.
        if (!click) return;

        // If the mouse is over the UI, exit.
        if (UIManager.getInstance().getHoveredObject() != null) return;

        Screen nextScreen = Screen.DEFAULT_SCREEN;

        if (mCurrentScreen != nextScreen) {
            switchScreen(nextScreen);
        }
    }

    /** If the user selects a tile, swap to the relevant screen. */
    private void detectTileSelection() {
        Player player = getPlayer();
        Cursor cursor = Actions.getCursor();
        if (player == null || cursor == null) return;

        if (cursor.hadLittleDrag()) return;

        if (Reference.isValid(UIManager.getInstance().getHoveredObject())) return;

        // Ensure a tile can be selected.
        HexagonMap map = player.getMap();
        if (map == null) return;

        // Select a tile.
        HexagonTile selected = map.cursorToTile();
        if (selected == null) return;

        // If in attack mode we select on hover for UI
        if (mCurrentScreen == Screen.ATTACKING_SCREEN) {
            Building building = selected.getBuilding();

            if (player.attackCheck(mHexChosen.getBuilding(), building)) {
                mBuildingChosen = building.getReference(Building.class);
            } else {
                mBuildingChosen = null;
            }
        }

        boolean click = GameActions.LEFT_CLICK.isJustDeactivated() && !cursor.hadLittleDrag();
        // If no clicking is occurring, exit.
        if (!click) return;

        // If the mouse is over the UI, exit.
        if (UIManager.getInstance().getHoveredObject() != null) return;

        switch (mCurrentScreen) {
            case ATTACKING_SCREEN:
                if (selected.hasBuilding()) {
                    Building building = selected.getBuilding();

                    if (building.getOwner() == player) {
                        mHexChosen = selected;
                    } else if (player.attackCheck(mHexChosen.getBuilding(), building)) {
                        player.getClientAttackRequest()
                                .invoke((data) -> data.setData(mHexChosen.getBuilding(), building));
                    }
                } else if (!selected.isClaimed()) {
                    mHexChosen = null;
                    switchScreen(Screen.DEFAULT_SCREEN);
                }
                break;
            case SELLING_SCREEN:
                if (mHexChosen == selected) {
                    break;
                }
                // fallthrough
            case DEFAULT_SCREEN:
            case BUILDING_SELECTED_SCREEN:
                mHexChosen = selected;

                Screen nextScreen = Screen.DEFAULT_SCREEN;

                if (mHexChosen.hasBuilding()) {
                    Building building = mHexChosen.getBuilding();
                    if (building == null) break;

                    // Select the building.
                    mBuildingChosen = building.getReference(Building.class);

                    if (player.isBuildingOwner(building)) {
                        nextScreen = Screen.BUILDING_SELECTED_SCREEN;
                    }
                }

                if (mCurrentScreen != nextScreen) {
                    switchScreen(nextScreen);
                }

                break;
            case PLACING_NEW_BUILDING:
                mHexChosen = selected;

                if (mHexChosen.isBuildable(player)) {
                    // place building
                    player.getClientBuildRequest()
                            .invoke(
                                    new BuildData(
                                            mHexChosen,
                                            PredefinedBuildings.getIndex(
                                                    mPredefinedBuildingChosen)));
                } else if (selected.hasBuilding() && selected.getClaimant() == player) {
                    mBuildingChosen = selected.getClaimedBy().getReference(Building.class);
                    switchScreen(Screen.BUILDING_SELECTED_SCREEN);
                }

                break;
        }
    }

    /** This updates what the user can see. */
    private void updateVisuals() {

        // Ensure Player and MapEffects exist.
        Player player = getPlayer();
        if (player == null) return;
        MapEffects effects = player.getMapEffects();
        if (effects == null) return;

        // Only run if visuals need updating.
        if (!mVisualsNeedUpdate) return;
        mVisualsNeedUpdate = false;

        if (player.hasLost()) return;

        // Set the player for the effects.
        effects.setActivePlayer(mPlayer);

        if (mCurrentScreen != Screen.ATTACKING_SCREEN) {
            mArcFadeOut = true;
        }

        switch (mCurrentScreen) {
            case DEFAULT_SCREEN:
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightHoveredTile(fx, StandardHighlightType.SELECT);
                        });
                break;
            case BUILDING_SELECTED_SCREEN:
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightSelectedTile(fx, StandardHighlightType.VALID);
                            highlightHoveredTile(fx, StandardHighlightType.SELECT);
                        });
                break;
            case ATTACKING_SCREEN:
                mArcFadeOut = false;

                if (!Reference.isValid(mUpdater)) {
                    mUpdater = new Reference<>(new ArcUpdater());
                    mArcPath.get().setUpdater(mUpdater.cast(IPathUpdater.class));
                    mArcPath.get().setArcHandler(mUpdater.cast(IArcHandler.class));
                }

                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightAttackableTiles(
                                    fx, StandardHighlightType.ATTACK, StandardHighlightType.SELECT);
                            highlightHoveredTile(fx, StandardHighlightType.SELECT);
                        });
                break;
            case SELLING_SCREEN:
                effects.setHighlightOverlay(
                        (fx) -> {
                            if (!Reference.isValid(mBuildingChosen)) {
                                return;
                            }

                            fx.highlightTile(
                                    mBuildingChosen.get().getTile(),
                                    StandardHighlightType.SELECT_INVALID.asSelection());
                            highlightHoveredTile(fx, StandardHighlightType.SELECT);
                        });
                break;
            case PLACING_NEW_BUILDING:
                effects.setHighlightOverlay(
                        (fx) -> {
                            highlightBuildableTiles(fx, StandardHighlightType.VALID);

                            HexagonMap map = player.getMap();

                            if (map == null) {
                                return;
                            }

                            HexagonTile hovered = map.cursorToTile();

                            if (hovered == null) {
                                return;
                            }

                            boolean valid =
                                    hovered.isBuildable(player)
                                            && mPredefinedBuildingChosen.getTotalCost(player)
                                                    <= player.getTokens().get();

                            StandardHighlightType hl =
                                    valid
                                            ? StandardHighlightType.SELECT
                                            : StandardHighlightType.SELECT_INVALID;

                            highlightHoveredTile(fx, hl);
                        });
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

    /**
     * Highlight hovered tile.
     *
     * @param effects map effects instance.
     * @param highlight colour of highlight.
     */
    private void highlightHoveredTile(MapEffects effects, StandardHighlightType highlight) {
        if (!Reference.isValid(mPlayer)
                || mIsPaused
                || Reference.isValid(UIManager.getInstance().getHoveredObject())) {
            return;
        }

        Cursor cursor = Actions.getCursor();

        if (cursor != null && cursor.hadLittleDrag()) {
            return;
        }

        Player player = mPlayer.get();

        HexagonMap map = player.getMap();

        if (map == null) {
            return;
        }

        effects.pulseHighlight(map.cursorToTile(), highlight.asSelection(), 0.6f, 2f, 0.05f);
    }

    /**
     * Highlight selected tile.
     *
     * @param effects map effects instance.
     * @param highlight highlight to use.
     */
    private void highlightSelectedTile(MapEffects effects, StandardHighlightType highlight) {
        if (mHexChosen == null || effects == null || highlight == null) return;
        effects.highlightTile(mHexChosen, highlight.asSelection());
    }

    /**
     * Highlight buildable tiles.
     *
     * @param fx map effects instance.
     * @param highlight highlight to use.
     */
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

    /**
     * Highlight surrounding Buildings based on the {@link #mHexChosen}.
     *
     * @param effects The MapEffects to be used.
     * @param attackHighlight Used to highlight attackable buildings.
     * @param selectHighlight Used when an attackable building is selected.
     */
    private void highlightAttackableTiles(
            MapEffects effects,
            StandardHighlightType attackHighlight,
            StandardHighlightType selectHighlight) {
        if (mHexChosen == null
                || effects == null
                || attackHighlight == null
                || selectHighlight == null) return;
        Player player = getPlayer();
        if (player == null || !mHexChosen.hasBuilding()) return;
        Building myBuilding = mHexChosen.getBuilding();

        // Highlight the player's building.
        effects.highlightTile(mHexChosen, selectHighlight.asSelection());

        Building targetBuilding = null;
        if (Reference.isValid(mBuildingChosen)) {
            targetBuilding = mBuildingChosen.get();
        }

        for (Building attackableBuilding : myBuilding.getAttackableBuildings()) {
            HexagonTile tile = attackableBuilding.getTile();

            // Highlight the building to attack in a different colour.
            if (targetBuilding != null && attackableBuilding.equals(targetBuilding)) {
                effects.highlightTile(tile, selectHighlight.asSelection());
                continue;
            }

            effects.highlightTile(tile, attackHighlight.asSelection());
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
}
