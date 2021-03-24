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
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.*;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
@Log
@Accessors(prefix = "m")
public class HumanPlayer extends Component implements IFrameUpdate, IFixedUpdate, IOnStart {

    // All screens to be used
    private Screen mScreenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mMapScreen;

    private Reference<UIMenuLeftDrawer> mMenuDrawer;

    // Data which is needed on different screens
    @Getter
    @Setter
    private HexagonTile mHexChosen;

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

    /**
     * The constructor for the human player
     */
    public HumanPlayer(Reference<NetworkManager> networkManager, int netID) {
        mNetworkManager = networkManager;
        mNetID = netID;
        mNetworkManager.get().getClientManager().registerSpawnListener(this::onSpawnObject);
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


        getGameObject().buildChild(
                "menu_drawer",
                new TransformUI(true),
                (go) -> {
                    mTokenCounterObject = go.buildChild("token_counter", new TransformUI(true),
                            (self) -> {
                                UITokenCounter tokenCounter = new UITokenCounter();
                                self.addComponent(tokenCounter);
                            });
                    go.addComponent(new UIMenuLeftDrawer(mBuildingChosen, this::getHexChosen, this::setHexChosen, this::setScreenOn));
                });

        mTokenCounter = mTokenCounterObject.get().getComponent(UITokenCounter.class);
        mMenuDrawer = getGameObject().getComponent(UIMenuLeftDrawer.class);
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
            mLocalTokens = mPlayer.get().getTokens().get();
            if (mTokenCounter != null && mTokenCounter.isValid()) {
                mTokenCounter.get().setLabelReference(mLocalTokens);
            } else {
                mTokenCounter = getGameObject().getComponent(UITokenCounter.class);
            }
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        // Choose which screen to show
        if (mScreenOn == Screen.MAP_SCREEN) {
            mMapScreen.get().setEnabled(mScreenOn == Screen.MAP_SCREEN);
        } else {
            if (mMenuDrawer != null && mMenuDrawer.isValid()) {
                mMenuDrawer.get().setMenu(mScreenOn);
            }
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
            if (UIManager.getInstance().getHoveredObject() == null
                    // this
                    // is breaking something
                    && mainCam != null) {
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
                    if (buildingOnTile.get() == null) {

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
                            Player owner = player.getTileOwner(tile);
                            if (owner != null) {
                                return owner.getPlayerHighlightSelection();
                            }
                            return HighlightSelection.CLEARED;
                        });
                break;
            case BUILDING_SELECTED_SCREEN:
                effects.highlightTiles(
                        (tile) -> {
                            if (tile == mHexChosen)
                                return StandardHighlightType.VALID.asSelection();
                            return HighlightSelection.CLEARED;
                        });
                break;
            case TILE_SCREEN:
                effects.highlightTiles(
                        (tile) -> {
                            if (tile == mHexChosen)
                                return StandardHighlightType.PLAIN.asSelection();
                            return HighlightSelection.CLEARED;
                        });
                break;
            case ATTACK_SCREEN:
                effects.highlightTiles(
                        (tile) -> {
                            return HighlightSelection.CLEARED;
                        });
                break;
            case STAT_SCREEN:
                break;
        }
    }

    /**
     * Marks visuals to update whenever a new object is spawned
     */
    private void onSpawnObject(NetworkObject obj) {
        if (obj.getGameObject().getComponent(Building.class) != null) mVisualsNeedUpdate = true;
    }

    private void setScreenOn(Screen newScreen) {
        if (!newScreen.equals(mScreenOn)) mVisualsNeedUpdate = true;
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

        return mPlayer.get().getBuilding(buildingToCheck.get().getTile()) != null;
    }

    private void buildBuildingSelectedView(GameObject go) {
        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
        // Choose to upgrade the buildingSelectedView
        go.buildChild(
                "Upgrade Button",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class).setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(
                            new UIRenderable(
                                    new SampledTexture(
                                            "ui/wide_button.png"))); // Make way to Go back
                    box.addComponent(
                            new UIButton(
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Upgrade Building -- Not completed"),
                                    (handle, __) -> {
                                        // TODO When clicked need to
                                        // show options to upgrade
                                        // buildingSelectedView stats.  Will leave
                                        // until after prototype

                                        setScreenOn(Screen.STAT_SCREEN);
                                    }));
                });
        // Choose to attackView a buildingSelectedView from here
        go.buildChild(
                "Attack buildingSelectedView",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class).setParentAnchor(0f, 0.15f, 0.5f, 0.15f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            new UIButton(
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Attack!"),
                                    (handle, __) -> {

                                        // Gets the buildingSelectedView to attackView
                                        // from and stored
                                        mBuildingChosen =
                                                new Reference<Building>(
                                                        mPlayer.get()
                                                                .getMapComponent()
                                                                .getBuilding(
                                                                        mHexChosen.getQ(),
                                                                        mHexChosen.getR()));
                                        setScreenOn(Screen.ATTACK_SCREEN);
                                    }));
                });
        // Sell a buildingSelectedView
        go.buildChild(
                "Sell buildingSelectedView",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class).setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            new UIButton(
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Sell Building -- Not Done"),
                                    (handle, __) -> {
                                        // TODO When clicked need to
                                        // sell buildingSelectedView

                                        mPlayer.get()
                                                .getClientSellRequest()
                                                .invoke(
                                                        new SellData(
                                                                mBuildingChosen
                                                                        .get())); // Send Data

                                        mBuildingChosen = null;
                                        mHexChosen = null;
                                        setScreenOn(Screen.MAP_SCREEN);
                                    }));
                });

        // Go Back
        go.buildChild(
                "Go Back",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class).setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            new UIButton(
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Go Back"),
                                    (handle, __) -> {
                                        mHexChosen = null;
                                        mBuildingChosen = null;
                                        setScreenOn(Screen.MAP_SCREEN);
                                    }));
                });
    } //TODO Creating in UIMENuLeftDrawer

    private void buildPlaceSelectedView(GameObject go) {
        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
        // Will build a box to confirm
        go.buildChild(
                "confirm box",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class).setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            // When clicked send the data to the server
                            new UIButton(
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Place Building"),
                                    (handle, __) -> {
                                        mPlayer.get()
                                                .getClientBuildRequest()
                                                .invoke(new BuildData(mHexChosen));

                                        mHexChosen = null;
                                        mBuildingChosen = null;
                                        setScreenOn(Screen.MAP_SCREEN);
                                    }));
                });
        // Go Back button
        go.buildChild(
                "Go Back",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class).setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            new UIButton(
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Go Back"),
                                    (handle, __) -> {
                                        mHexChosen = null;
                                        mBuildingChosen = null;
                                        setScreenOn(Screen.MAP_SCREEN);
                                    }));
                });
    } //TODO Creating in UIMENuLeftDrawer
}
