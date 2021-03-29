/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

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
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
@Log
public class HumanPlayer extends Component implements IFrameUpdate, IFixedUpdate, IOnStart {

    // All screens to be used
    private Screen mScreenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mMapScreen;
    private Reference<GameObject> mPlaceScreen;
    private Reference<GameObject> mBuildingSelectedScreen;
    private Reference<GameObject> mChooseAttack;
    private Reference<GameObject> mShowStat;
    private Reference<GameObject> mTokenBanner;
    private Reference<UIButton> mTokenBannerButton;

    // Data which is needed on different screens
    private HexagonTile mHexChosen;
    private Reference<Building> mBuildingChosen = new Reference<Building>(null);

    // The player
    private Reference<Player> mPlayer;
    private int mLocalTokens = 0;

    private final int mNetID;
    private final Reference<NetworkManager> mNetworkManager;

    // Visual effects
    private Reference<MapEffects> mMapEffects;
    private boolean mVisualsNeedUpdate;

    /**
     * Create a {@link HumanPlayer}.
     *
     * @param networkManager The network manager.
     * @param netID The human player's network ID.
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
                getGameObject().buildChild("map screen", new TransformUI(), this::mapScreenView);

        // Get the screen for confirming placing a buildingSelectedView
        mPlaceScreen =
                getGameObject()
                        .buildChild(
                                "place screen", new TransformUI(), this::buildPlaceSelectedView);

        // Screen to choose what to do for a buildingSelectedView
        mBuildingSelectedScreen =
                getGameObject()
                        .buildChild(
                                "buildingSelectedView options",
                                new TransformUI(),
                                this::buildBuildingSelectedView);

        // To Attack
        mChooseAttack =
                getGameObject()
                        .buildChild("attackView screen", new TransformUI(), this::buildAttackView);

        // To upgrade stats
        mShowStat = getGameObject().buildChild("Stat screen", new TransformUI(), this::statView);

        mTokenBanner = getGameObject().buildChild("token_view", new TransformUI(), this::tokenView);
        mTokenBanner.get().setEnabled(true);
    }

    @Override
    protected void onDestroy() {}

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
            if (mTokenBannerButton.isValid()) {
                Reference<UIText> txt = mTokenBannerButton.get().getLabelText();
                if (txt != null && txt.isValid()) {
                    txt.get().setText("Tokens: " + mLocalTokens);
                }
            }
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {

        // Choose which screen to show
        mMapScreen.get().setEnabled(mScreenOn == Screen.MAP_SCREEN);
        mPlaceScreen.get().setEnabled(mScreenOn == Screen.TILE_SCREEN);
        mBuildingSelectedScreen.get().setEnabled(mScreenOn == Screen.BUILDING_SELECTED_SCREEN);
        mChooseAttack.get().setEnabled(mScreenOn == Screen.ATTACK_SCREEN);
        mShowStat.get().setEnabled(mScreenOn == Screen.STAT_SCREEN);
        if (mScreenOn == Screen.MAP_SCREEN) {
            mapScreen();
        }

        if (mVisualsNeedUpdate) updateVisuals();
    }

    /** This will choose what to do when the user can see the full map */
    private void mapScreen() {

        // Checks that its clicking something
        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);
        if (mPlayer != null
                && GameActions.LEFT_CLICK.isActivated() //                &&
                // UIManager.getInstance().getHoveredObject() == null, this
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

            if (mHexChosen != null) {
                if (mHexChosen.hasBuilding()) {
                    Building building = mHexChosen.getBuilding();

                    if (hasPlayerGotBuilding(building.getReference(Building.class))) {
                        mBuildingChosen = building.getReference(Building.class);
                        setScreenOn(Screen.BUILDING_SELECTED_SCREEN);
                    }
                } else {
                    // Checks if cannot build here
                    if (mPlayer.get()
                            .buildingWithinRadius(mPlayer.get().getTilesInRadius(1, mHexChosen))) {
                        System.out.println("Human:Cannot build");
                        mHexChosen = null;
                        mBuildingChosen = null;
                        return;
                    } else {
                        // If you can build
                        System.out.println("Human:Change Screen");
                        setScreenOn(Screen.TILE_SCREEN);
                    }
                }
            }
        }
    }

    /** AURI!! This updates what the user can see */
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

    /** Marks visuals to update whenever a new object is spawned */
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

        return mPlayer.get().getOwnedBuilding(buildingToCheck.get().getTile()) != null;
    }

    /**
     * This is a function which outputs what the user should see on a map
     *
     * @param go The {@code GameObject} to build off
     */
    private void buildAttackView(GameObject go) {

        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

        // If its equal to null ignore
        if (mBuildingChosen.get() == null) {;
        } else {
            // For each Building add a button for it
            for (Building building : mBuildingChosen.get().getAttackableBuildings()) {

                go.buildChild(
                        "Attack buildingSelectedView",
                        new TransformUI(true),
                        (box) -> {
                            box.getTransform(TransformUI.class)
                                    .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                            box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                            box.addComponent(
                                    new UIRenderable(new SampledTexture("ui/wide_button.png")));
                            box.addComponent(
                                    new UIButton(
                                            new UIText(
                                                    new Vector3f(0f, 0f, 0f),
                                                    Font.getFontResource("Rise of Kingdom.ttf"),
                                                    "Attack buildingSelectedView "), // TODO (After
                                            // Prototype)
                                            // -- Need way to show
                                            // different buildingSelectedView
                                            (handle, __) -> {

                                                // Send attackView to server
                                                mPlayer.get()
                                                        .getClientAttackRequest()
                                                        .invoke(
                                                                new AttackData(
                                                                        mBuildingChosen.get(),
                                                                        building)); // TODO Send
                                                // data to
                                                // this which

                                                mHexChosen = null;
                                                mBuildingChosen = null;
                                                setScreenOn(Screen.MAP_SCREEN);
                                            }));
                        });
            }
        }

        // Back Button
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
    }

    /**
     * This will create the screen which gives the actions of what a player can do once they have
     * clicked a building
     *
     * @param go The {@code GameObject} to build off
     */
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
                                        if (mHexChosen != null && mHexChosen.hasBuilding()) {
                                            mBuildingChosen =
                                                    mHexChosen
                                                            .getBuilding()
                                                            .getReference(Building.class);
                                        }
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
    }

    /**
     * This will create the screen which you can build a building offf
     *
     * @param go The {@code GameObject} to build off
     */
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
    }

    /**
     * This screen allows the user to interact with the map
     *
     * @param go The {@code GameObject} to build off
     */
    private void mapScreenView(GameObject go) {

        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
    }

    /**
     * This will allow the user to upgrade stats
     *
     * @param go The {@code GameObject} to build off
     */
    private void statView(GameObject go) {

        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

        ; // TODO will add stuff for Stats AFTER prototype

        go.buildChild(
                "Go Back",
                new TransformUI(true),
                (box) -> {
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
    }

    private void tokenView(GameObject go) {

        go.addComponent(new UIRenderable(new Vector4f(0f, 0f, 0f, 0f)));

        ; // TODO will add stuff for Stats AFTER prototype

        Reference<GameObject> tmp_ref =
                go.buildChild(
                        "token_count",
                        new TransformUI(true),
                        (box) -> {
                            box.getTransform(TransformUI.class)
                                    .setParentAnchor(0f, 0.01f, 0.5f, 0.01f);
                            box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                            box.addComponent(
                                    new UIRenderable(new SampledTexture("ui/wide_button.png")));
                            box.addComponent(
                                    new UIButton(
                                            new UIText(
                                                    new Vector3f(0f, 0f, 0f),
                                                    Font.getFontResource("Rise of Kingdom.ttf"),
                                                    "Tokens: " + mLocalTokens)));
                        });

        mTokenBannerButton = tmp_ref.get().getComponent(UIButton.class);
    }
}
