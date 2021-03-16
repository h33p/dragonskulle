/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.joml.Vector2d;
import org.joml.Vector4f;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
public class HumanPlayer extends Component implements IFrameUpdate, IOnStart {

	// All screens to be used
    private Screen mScreenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mMapScreen;
    private Reference<GameObject> mPlaceScreen;
    private Reference<GameObject> mBuildingScreen;
    private Reference<GameObject> mChooseAttack;
    private Reference<GameObject> mShowStat;
    
    // Data which is needed on different screens
    private Reference<HexagonTile> mHexChosen;
    private Reference<Building> mBuildingChosen = new Reference<Building>(null);

    // The player 
    private Reference<Player> mPlayer;

    /** The constructor for the human player */
    public HumanPlayer() {}

    @Override
    public void onStart() {
        
    	//Get the player
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
                                    // TODO How to click hex

                                }); // This will draw a rectangle to the screen.  Need way to change
        // screen

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
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                		// When clicked send the data to the server
                                                        new UIButton(
                                                                (handle, __) -> {
                                                                    mPlayer.get()
                                                                            .mClientBuildRequest
                                                                            .invoke(
                                                                                    new BuildData(
                                                                                            mHexChosen
                                                                                                    .get()));

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
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
                                                                (handle, __) -> {
                                                                    mHexChosen = null;
                                                                    mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
                                });

        // Screen to choose what to do for a building
        mBuildingScreen =
        		
                getGameObject()
                        .buildChild(
                                "building options",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
                                    // Choose to upgrade the building
                                    go.buildChild(
                                            "Upgrade Button",
                                            new TransformUI(true),
                                            (box) -> {
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png"))); // Make way to Go back
                                                box.addComponent(
                                                        new UIButton(
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
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
                                                                (handle, __) -> {
                                                                    
                                                                	// Gets the building to attack from and stored
                                                                    mBuildingChosen =
                                                                            new Reference<Building>(
                                                                                    mPlayer.get()
                                                                                            .getMapComponent()
                                                                                            .get()
                                                                                            .getBuilding(
                                                                                                    mHexChosen
                                                                                                            .get()
                                                                                                            .getQ(),
                                                                                                    mHexChosen
                                                                                                            .get()
                                                                                                            .getR()));
                                                                    mScreenOn =
                                                                            Screen.ATTACK_SCREEN;
                                                                }));
                                            });
                                    // Sell a building
                                    go.buildChild(
                                            "Sell building",
                                            new TransformUI(true),
                                            (box) -> {
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
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
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
                                                                (handle, __) -> {
                                                                    mHexChosen = null;
                                                                    mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
                                });

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
                                                                (handle, __) -> {
                                                                    mHexChosen = null;
                                                                    mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
                                });
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void frameUpdate(float deltaTime) {
    	// Update token
        mPlayer.get().updateTokens(deltaTime);

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

    /** This will choose what to do when the user can see the full map */
    private void mapScreen() {
    	
    	// Checks that its clicking somehting 
        if (GameActions.LEFT_CLICK.isActivated()
                && UIManager.getInstance().getHoveredObject() == null) {
            Vector2d cursorPosition = GameActions.getCursor().getPosition();
            // TODO Check which tile has been selected Auri has said he will convert from screen to
            // local.

            mHexChosen = null; // TODO Work out which one chosen

            // When chosen a hexagon
            if (mHexChosen != null) {
            	
            	// Gets reference to building
                Reference<Building> buildingOnTile =
                        new Reference<Building>(
                                mPlayer.get()
                                        .getMapComponent()
                                        .get()
                                        .getBuilding(
                                                mHexChosen.get().getQ(), mHexChosen.get().getR()));
                
                // If there is a building there
                if (buildingOnTile.get() == null) {

                    // Checks if cannot build here
                    if (mPlayer.get()
                            .buildingWithinRadius(
                                    mPlayer.get().getTilesInRadius(1, mHexChosen.get()))) {
                        mHexChosen = null;
                        mBuildingChosen = null;
                        return;
                        // If you can build
                    } else {
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
     * @param go The game object
     */
    private void attack(GameObject go) {

        go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

        // If its equal to null ignore 
        if (mBuildingChosen.get() == null) {
        	;
        }
        else {
        	// For each Building add a button for it
        for (Building building : mBuildingChosen.get().getAttackableBuildings()) {
        	
            go.buildChild(
                    "Attack building",
                    new TransformUI(true),
                    (box) -> {
                        box.addComponent(
                                new UIRenderable(new SampledTexture("ui/wide_button.png")));
                        box.addComponent(
                                new UIButton(
                                        (handle, __) -> {

                                        	// Send attack to server
                                        	mPlayer.get()
                                                    .mClientAttackRequest
                                                    .invoke(
                                                            new AttackData(
                                                                    mBuildingChosen.get(),
                                                                    building)); // TODO Send data to
                                            // this which

                                            mHexChosen = null;
                                            mBuildingChosen = null;
                                            mScreenOn = Screen.MAP_SCREEN;
                                        }));
                    });
        }}
        
        // Back Button
        go.buildChild(
                "Go Back",
                new TransformUI(true),
                (box) -> {
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(
                            new UIButton(
                                    (handle, __) -> {
                                        mHexChosen = null;
                                        mBuildingChosen = null;
                                        mScreenOn = Screen.MAP_SCREEN;
                                    }));
                });
    }
}
