/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.HighlightSelection;
import org.dragonskulle.game.map.MapEffects.StandardHighlightType;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.*;
import org.joml.Vector2fc;
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
    private Screen mScreenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mMapScreen;

    private Reference<UIMenuLeftDrawer> mMenuDrawer;

    // Data which is needed on different screens
    @Getter
    @Setter
    private HexagonTile mHexChosen;

    @Getter
    @Setter
    private Reference<Building> mBuildingChosen = new Reference<>(null);

    // The player
    private Reference<Player> mPlayer;
    private int mLocalTokens = 0;

    private final int mNetID;
    private final Reference<NetworkManager> mNetworkManager;

    // Visual effects
    private Reference<MapEffects> mMapEffects;
    private boolean mVisualsNeedUpdate;
    private Reference<GameObject> mZoomSlider;
    private Reference<UITokenCounter> mTokenCounter;
    private Reference<GameObject> mTokenCounterObject;
    private HexagonTile mLastHexChosen;

    /**
     * The constructor for the human player
     */
    public HumanPlayer(Reference<NetworkManager> networkManager, int netID) {
        mNetworkManager = networkManager;
        mNetID = netID;
        mNetworkManager.get().getClientManager().registerSpawnListener(this::onSpawnObject);
        mNetworkManager
                .get()
                .getClientManager()
                .registerOwnershipModificationListener(this::onOwnerModifiedObject);
    }

    @Override
    public void onStart() {

        mMapEffects =
                Scene.getActiveScene()
                        .getSingleton(MapEffects.class)
                        .getReference(MapEffects.class);
        mVisualsNeedUpdate = true;

        // Get the screen for map
        mMapScreen =
                // Creates a blank screen
                getGameObject().buildChild("map screen", new TransformUI(), (go) -> {
                });

        mZoomSlider =
                // Creates a blank screen
                getGameObject()
                        .buildChild(
                                "zoom_slider",
                                new TransformUI(true),
                                (go) -> {
                                    go.addComponent(new UILinkedScrollBar());
                                });

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
                                                UITokenCounter tokenCounter = new UITokenCounter();
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
        mMenuDrawer = getGameObject().getComponent(UIMenuLeftDrawer.class);
    }

    private Reference<Player> getPlayer() {
        return this.mPlayer;
    }

    @Override
    protected void onDestroy() {
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        // Try getting the player if haven't already
        if (mPlayer == null) {
            NetworkManager manager = mNetworkManager.get();

            if (manager != null && manager.getClientManager() != null)
                mPlayer =
                        manager.getClientManager()
                                .getNetworkObjects()
                                .filter(Reference::isValid)
                                .map(Reference::get)
                                .filter(NetworkObject::isMine)
                                .map(NetworkObject::getGameObject)
                                .map(go -> go.getComponent(Player.class))
                                .filter(ref -> ref != null)
                                .findFirst()
                                .orElse(null);
        }

        if (mPlayer == null) return;

        // Update token
        if (mPlayer.isValid()) {
            updateVisibleTokens();
        }
    }

    private void updateVisibleTokens() {
        mLocalTokens = mPlayer.get().getTokens().get();
        if (mTokenCounter != null && mTokenCounter.isValid()) {
            mTokenCounter.get().setLabelReference(mLocalTokens);
        } else {
            mTokenCounter = getGameObject().getComponent(UITokenCounter.class);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        // Choose which screen to show

        if (mMenuDrawer != null && mMenuDrawer.isValid()) {
            mMenuDrawer.get().setMenu(mScreenOn);
        }

        mapScreen();

        if (mVisualsNeedUpdate) updateVisuals();
    }

    /**
     * This will choose what to do when the user can see the full map
     */
    private void mapScreen() {

        // Checks that its clicking something
        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);
        if (GameActions.LEFT_CLICK.isActivated()) {
            if (UIManager.getInstance().getHoveredObject() == null && mainCam != null) {
                // Retrieve scaled screen coordinates
                Vector2fc screenPos = UIManager.getInstance().getScaledCursorCoords();
                // Convert those coordinates to local coordinates within the map
                Vector3f pos =
                        mainCam.screenToPlane(
                                mPlayer.get().getMapComponent().getGameObject().getTransform(),
                                screenPos.x(),
                                screenPos.y(),
                                new Vector3f());

                // Convert those coordinates to axial
                TransformHex.cartesianToAxial(pos);
                // And round them
                TransformHex.roundAxial(pos);
                // And then select the tile
                Player player = mPlayer.get();
                if (player != null) {
                    HexagonMap component = player.getMapComponent();
                    if (component != null) {
                        mLastHexChosen = mHexChosen;
                        mHexChosen = component.getTile((int) pos.x, (int) pos.y);
                    }
                }

                log.info("Human:Got the Hexagon to enter");

                // When chosen a hexagon
                if (mHexChosen != null) {

                    // Gets reference to buildingSelectedView
                    Reference<Building> buildingOnTile =
                            new Reference<Building>(
                                    mPlayer.get()
                                            .getMapComponent()
                                            .getBuilding(mHexChosen.getQ(), mHexChosen.getR()));

                    // If there is a buildingSelectedView there
                    if (!buildingOnTile.isValid()) {

                        // Checks if cannot build here
                        if (mPlayer.get()
                                .buildingWithinRadius(
                                        mPlayer.get().getTilesInRadius(1, mHexChosen))) {
                            System.out.println("Human:Cannot build");
                            mHexChosen = null;
                            mBuildingChosen = null;
                            return;
                            // If you can build
                        } else {
                            System.out.println("Human:Change Screen");
                            setScreenOn(Screen.TILE_SCREEN);
                        }
                        // Checks if the player owns the buildingSelectedView
                    } else if (hasPlayerGotBuilding(buildingOnTile)) {
                        mBuildingChosen = buildingOnTile;
                        setScreenOn(Screen.BUILDING_SELECTED_SCREEN);
                    } else {
                        return;
                    }
                }
            }
        } else if (GameActions.RIGHT_CLICK.isActivated()) {
            Vector2fc screenPos = UIManager.getInstance().getScaledCursorCoords();
            // Convert those coordinates to local coordinates within the map
            Vector3f pos =
                    mainCam.screenToPlane(
                            mPlayer.get().getMapComponent().getGameObject().getTransform(),
                            screenPos.x(),
                            screenPos.y(),
                            new Vector3f());

            System.out.println("[DEBUG] RCL Position : " + screenPos.toString());
            System.out.println("[DEBUG] RCL Position From Camera : " + pos.toString());
        }
    }

    /**
     * AURI!! This updates what the user can see
     */
    private void updateVisuals() {
        mVisualsNeedUpdate = false;

        if (mMapEffects == null || mPlayer == null) return;

        Player player = mPlayer.get();

        if (player == null) return;

        MapEffects effects = mMapEffects.get();

        switch (mScreenOn) {
            case MAP_SCREEN:
                log.info("UPDATE MAP SCREEN");
                effects.highlightTiles(
                        (tile) -> {
                            Player owner = tile.getClaimant();
                            if (owner != null) {
                                return owner.getPlayerHighlightSelection();
                            }
                            return HighlightSelection.CLEARED;
                        });
                break;
            case BUILDING_SELECTED_SCREEN:
                undoLastHighlight();
                highlightSelectedTile(StandardHighlightType.VALID);
                break;
            case TILE_SCREEN:
                undoLastHighlight();
                highlightSelectedTile(StandardHighlightType.PLAIN);
                break;
            case ATTACK_SCREEN:
                highlightSelectedTile(StandardHighlightType.VALID);
                for (Building attackableBuilding : mHexChosen.getBuilding().getAttackableBuildings()) {
                    effects.highlightTile(attackableBuilding.getTile(), StandardHighlightType.ATTACK.asSelection());
                }
                break;
            case STAT_SCREEN:
                break;
        }
    }

    private void highlightSelectedTile(StandardHighlightType highlight) {
        if (mHexChosen != null) {
            mMapEffects.get().highlightTile(mHexChosen, highlight.asSelection());
        }
    }

    private void undoLastHighlight() {
        MapEffects effects = mMapEffects.get();
        if (effects != null && mLastHexChosen != null) {
            final Player lastTileOwner = mPlayer.get().getTileOwner(mLastHexChosen);
            if (lastTileOwner != null) {
                effects.highlightTile(
                        mLastHexChosen,
                        lastTileOwner
                                .getPlayerHighlightSelection()); // set tile to what it was before
            } else {
                effects.unhighlightTile(mLastHexChosen);
            }
        }
    }

    /**
     * Marks visuals to update whenever a new object is spawned
     */
    private void onSpawnObject(NetworkObject obj) {
        if (obj.getGameObject().getComponent(Building.class) != null) mVisualsNeedUpdate = true;
    }

    /** Marks visuals to update whenever a new object is spawned */
    private void onOwnerModifiedObject(Reference<NetworkObject> obj) {
        // remove from self as owned if exists, then we need to check if we are the owner again
        if (obj.isValid()) {
            final Reference<Building> buildingReference =
                    obj.get().getGameObject().getComponent(Building.class);
            if (obj.get().isMine()) {
                mPlayer.get().addOwnership(buildingReference);
            } else if (buildingReference != null
                    && mPlayer.get().thinksOwnsBuilding(buildingReference)) {
                mPlayer.get().removeFromOwnedBuildings(buildingReference);
            }
        }
    }

    /**
     * AURI!!!
     *
     * @param newScreen
     */
    private void setScreenOn(Screen newScreen) {
        if (!newScreen.equals(mScreenOn) || (mLastHexChosen != mHexChosen))
            mVisualsNeedUpdate = true;
        mScreenOn = newScreen;
    }

    /**
     * A Method to check if the player owns that buildingSelectedView or not
     *
     * @param buildingToCheck The buildingSelectedView to check
     * @return true if the player owns the buildingSelectedView, false if not
     */
    private boolean hasPlayerGotBuilding(Reference<Building> buildingToCheck) {
        if (buildingToCheck == null || !buildingToCheck.isValid()) return false;

        return mPlayer.get().getOwnedBuilding(buildingToCheck.get().getTile()) != null;
    }
}
