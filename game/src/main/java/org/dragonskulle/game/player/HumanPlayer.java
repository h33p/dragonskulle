/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import lombok.extern.java.Log;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
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
    private Reference<GameObject> mBuildingScreen;
    private Reference<GameObject> mChooseAttack;
    private Reference<GameObject> mShowStat;
    private Reference<GameObject> mTokenBanner;

    // Data which is needed on different screens
    private HexagonTile mHexChosen;
    private Reference<Building> mBuildingChosen = new Reference<Building>(null);

    // The player
    private Reference<Player> mPlayer;
    private int mLocalTokens = 0;

    /**
     * The constructor for the human player
     */
    public HumanPlayer() {
    }

    @Override
    public void onStart() {

        // Get the player
        mPlayer = getGameObject().getComponent(Player.class);

        // Get the screen for map
        mMapScreen =
                // Creates a blank screen
                getGameObject()
                        .buildChild(
                                "map screen",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
                                });

        // Get the screen for confirming placing a building
        mPlaceScreen =
                getGameObject()
                        .buildChild(
                                "place screen",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
                                    // Will build a box to confirm
                                    go.buildChild(
                                            "confirm box",
                                            new TransformUI(true),
                                            (box) -> {
                                                box.getTransform(TransformUI.class)
                                                        .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                                                box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        // When clicked send the data to the server
                                                        new UIButton(
                                                                new UIText(
                                                                        new Vector3f(0f, 0f, 0f),
                                                                        Font.getFontResource(
                                                                                "Rise of Kingdom.ttf"),
                                                                        "Place Building"),
                                                                (handle, __) -> {
                                                                    mPlayer.get()
                                                                            .mClientBuildRequest
                                                                            .invoke(
                                                                                    new BuildData(
                                                                                            mHexChosen));

                                                                    mHexChosen = null;
                                                                    mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
                                    // Go Back button
                                    go.buildChild(
                                            "Go Back",
                                            new TransformUI(true),
                                            (box) -> {
                                                box.getTransform(TransformUI.class)
                                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                                box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
                                                                new UIText(
                                                                        new Vector3f(0f, 0f, 0f),
                                                                        Font.getFontResource(
                                                                                "Rise of Kingdom.ttf"),
                                                                        "Go Back"),
                                                                (handle, __) -> {
                                                                    mHexChosen = null;
                                                                    mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
                                });

        // Screen to choose what to do for a building
        mBuildingScreen =
                getGameObject().buildChild("building options", new TransformUI(), this::building);

        // To Attack
        mChooseAttack =
                getGameObject().buildChild("attack screen", new TransformUI(), this::attack);

        // To upgrade stats
        mShowStat =
                getGameObject()
                        .buildChild(
                                "Stat screen",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

                                    ; // TODO will add stuff for Stats AFTER prototype

                                    go.buildChild(
                                            "Go Back",
                                            new TransformUI(true),
                                            (box) -> {
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
                                                                new UIText(
                                                                        new Vector3f(0f, 0f, 0f),
                                                                        Font.getFontResource(
                                                                                "Rise of Kingdom.ttf"),
                                                                        "Go Back"),
                                                                (handle, __) -> {
                                                                    mHexChosen = null;
                                                                    mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
                                });

        mTokenBanner =
                getGameObject().buildChild("token_view", new TransformUI(),
                        (go) -> {
                            go.addComponent(
                                    new UIRenderable(new Vector4f(0f, 0f, 0f, 0f)));

                            ; // TODO will add stuff for Stats AFTER prototype

                            go.buildChild(
                                    "token_count",
                                    new TransformUI(true),
                                    (box) -> {
                                        box.addComponent(
                                                new UIRenderable(
                                                        new SampledTexture(
                                                                "ui/wide_button.png")));
                                        box.addComponent(
                                                new UIButton(
                                                        new UIText(
                                                                new Vector3f(0f, 0f, 0f),
                                                                Font.getFontResource(
                                                                        "Rise of Kingdom.ttf"),
                                                                "Your tokens are " + mLocalTokens)
                                                ));


                                    });
                        });
        mTokenBanner.get().setEnabled(true);

    }

    @Override
    protected void onDestroy() {
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        // Update token
        mPlayer.get().updateTokens(deltaTime);
        mLocalTokens = mPlayer.get().getTokens().get();
    }

    @Override
    public void frameUpdate(float deltaTime) {

        // Choose which screen to show
        mMapScreen.get().setEnabled(mScreenOn == Screen.MAP_SCREEN);
        mPlaceScreen.get().setEnabled(mScreenOn == Screen.TILE_SCREEN);
        mBuildingScreen.get().setEnabled(mScreenOn == Screen.BUILDING_SCREEN);
        mChooseAttack.get().setEnabled(mScreenOn == Screen.ATTACK_SCREEN);
        mShowStat.get().setEnabled(mScreenOn == Screen.STAT_SCREEN);
        if (mScreenOn == Screen.MAP_SCREEN) {
            mapScreen();
        }
    }

    /**
     * This will choose what to do when the user can see the full map
     */
    private void mapScreen() {

        // Checks that its clicking something
        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);
        if (GameActions.LEFT_CLICK.isActivated() //                && UIManager.getInstance().getHoveredObject() == null, this is breaking something
                && mainCam != null) {
            // Retrieve scaled screen coordinates
            Vector2fc screenPos = UIManager.getInstance().getScaledCursorCoords();
            // Convert those coordinates to local coordinates within the map
            Vector3f pos =
                    mainCam.screenToPlane(
                            mPlayer.get().getMapComponent().get().getGameObject().getTransform(),
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
                Reference<HexagonMap> component = player.getMapComponent();
                if (component != null) {
                    mHexChosen = component.get().getTile((int) pos.x, (int) pos.y);
                }
            }

            System.out.println("Human:Got the Hexagon to enter");

            // When chosen a hexagon
            if (mHexChosen != null) {

                // Gets reference to building
                Reference<Building> buildingOnTile =
                        new Reference<Building>(
                                mPlayer.get()
                                        .getMapComponent()
                                        .get()
                                        .getBuilding(mHexChosen.getQ(), mHexChosen.getR()));

                // If there is a building there
                if (buildingOnTile.get() == null) {

                    // Checks if cannot build here
                    if (mPlayer.get()
                            .buildingWithinRadius(mPlayer.get().getTilesInRadius(1, mHexChosen))) {
                        System.out.println("Human:Cannot build");
                        mHexChosen = null;
                        mBuildingChosen = null;
                        return;
                        // If you can build
                    } else {
                        System.out.println("Human:Change Screen");
                        mScreenOn = Screen.TILE_SCREEN;
                    }
                    // Checks if the player owns the building
                } else if (hasPlayerGotBuilding(buildingOnTile)) {
                    mBuildingChosen = buildingOnTile;
                    mScreenOn = Screen.BUILDING_SCREEN;
                } else {
                    return;
                }
            }
        }
    }

    /**
     * A Method to check if the player owns that building or not
     *
     * @param buildingToCheck The building to check
     * @return true if the player owns the building, false if not
     */
    private boolean hasPlayerGotBuilding(Reference<Building> buildingToCheck) {

        // Goes through all buildings and check that the two buildings are equal
        for (int i = 0; i < mPlayer.get().numberOfBuildings(); i++) {
            Reference<Building> building = mPlayer.get().getBuilding(i);

            if (building == buildingToCheck) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is a function which outputs what the user should see on a map
     *
     * @param go The game object
     */
    private void attack(GameObject go) {

        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

        // If its equal to null ignore
        if (mBuildingChosen.get() == null) {
            ;
        } else {
            // For each Building add a button for it
            for (Building building : mBuildingChosen.get().getAttackableBuildings()) {

                go.buildChild(
                        "Attack building",
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
                                                    "Attack building "), // TODO (After Prototype)
                                            // -- Need way to show
                                            // different building
                                            (handle, __) -> {

                                                // Send attack to server
                                                mPlayer.get()
                                                        .mClientAttackRequest
                                                        .invoke(
                                                                new AttackData(
                                                                        mBuildingChosen.get(),
                                                                        building)); // TODO Send
                                                // data to
                                                // this which

                                                mHexChosen = null;
                                                mBuildingChosen = null;
                                                mScreenOn = Screen.MAP_SCREEN;
                                            }));
                        });
            }
        }

        // Back Button
        go.buildChild(
                "Go Back",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class)
                            .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
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
                                        mScreenOn = Screen.MAP_SCREEN;
                                    }));
                });
    }

    private void building(GameObject go) {
        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
        // Choose to upgrade the building
        go.buildChild(
                "Upgrade Button",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class)
                            .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
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
                                        // building stats.  Will leave
                                        // until after prototype

                                        mScreenOn = Screen.STAT_SCREEN;
                                    }));
                });
        // Choose to attack a building from here
        go.buildChild(
                "Attack building",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class)
                            .setParentAnchor(0f, 0.15f, 0.5f, 0.15f);
                    box.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            new UIButton(new UIText(
                                    new Vector3f(0f, 0f, 0f),
                                    Font.getFontResource("Rise of Kingdom.ttf"),
                                    "Attack!"),
                                    (handle, __) -> {

                                        // Gets the building to attack
                                        // from and stored
                                        mBuildingChosen =
                                                new Reference<Building>(
                                                        mPlayer.get()
                                                                .getMapComponent()
                                                                .get()
                                                                .getBuilding(
                                                                        mHexChosen.getQ(),
                                                                        mHexChosen.getR()));
                                        mScreenOn = Screen.ATTACK_SCREEN;
                                    }));
                });
        // Sell a building
        go.buildChild(
                "Sell building",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class)
                            .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
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
                                        // sell building

                                        mPlayer.get()
                                                .mClientSellRequest
                                                .invoke(
                                                        new SellData(
                                                                mBuildingChosen
                                                                        .get())); // Send Data

                                        mBuildingChosen = null;
                                        mHexChosen = null;
                                        mScreenOn = Screen.MAP_SCREEN;
                                    }));
                });

        // Go Back
        go.buildChild(
                "Go Back",
                new TransformUI(true),
                (box) -> {
                    box.getTransform(TransformUI.class)
                            .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
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
                                        mScreenOn = Screen.MAP_SCREEN;
                                    }));
                });
    }
}
