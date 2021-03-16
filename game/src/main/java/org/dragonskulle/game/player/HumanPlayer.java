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
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
public class HumanPlayer extends Component implements IFrameUpdate, IOnStart {

    private Screen mScreenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mMapScreen;
    private Reference<GameObject> mPlaceScreen;
    private Reference<GameObject> mBuildingScreen;
    private Reference<GameObject> mChooseAttack;
    private Reference<GameObject> mShowStat;

    private Reference<Player> mPlayer;

    private HexagonTile mHexChosen;
    private Building mBuildingChosen;

    /** The constructor for the human player */
    public HumanPlayer() {}

    @Override
    public void onStart() {
        // TODO Auto-generated method stub
    	
    	

        mMapScreen =
                getGameObject()  // Should work its lombok.
                        .buildChild(
                                "map screen",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
                                    // TODO How to click hex

                                }); // This will draw a rectangle to the screen.  Need way to change
        // screen

        mPlaceScreen =
                getGameObject()
                        .buildChild(
                                "place screen",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
                                    go.buildChild(
                                            "confirm box",
                                            new TransformUI(true),
                                            (box) -> {
                                                box.addComponent(
                                                        new UIRenderable(
                                                                new SampledTexture(
                                                                        "ui/wide_button.png")));
                                                box.addComponent(
                                                        new UIButton(
                                                                (handle, __) -> {
                                                                                                                                    	
                                                                	mPlayer.get().clientInvokeEvent(new BuildData(mHexChosen));

                                                                	mHexChosen = null;
                                                                	mBuildingChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
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

        mBuildingScreen =
                getGameObject()
                        .buildChild(
                                "building options",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
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
                                                                    // building stats.  Will leave until after prototype
                                                                	
                                                                    mScreenOn = Screen.STAT_SCREEN;
                                                                }));
                                            });
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
                                                                    // TODO When clicked need to
                                                                    // show buildings which can be
                                                                    // attacked -- get off building
                                                                	
                                                                	mBuildingChosen = mPlayer.get().getBuilding(mHexChosen.getQ(), mHexChosen.getR());
                                                                    mScreenOn =
                                                                            Screen.ATTACK_SCREEN;
                                                                }));
                                            });
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
                                                                    .clientInvokeEvent(
                                                                            new SellData(
                                                                                    mBuildingChosen)); // Send Data
                                                                	
                                                                	mBuildingChosen = null;
                                                                	mHexChosen = null;
                                                                    mScreenOn = Screen.MAP_SCREEN;
                                                                }));
                                            });
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

        mChooseAttack =
                getGameObject()
                        .buildChild(
                                "attack screen",
                                new TransformUI(),
                                (go) -> {
                                    go.addComponent(
                                            new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

                                    for (Building building :
                                            mBuildingChosen.getAttackableBuildings()) {
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
                                                                        // TODO When clicked need to
                                                                        // attack building
                                                                    	
                                                                    	mPlayer.get()
                                                                        .clientInvokeEvent(
                                                                                new AttackData(
                                                                                        mBuildingChosen, building)); // TODO Send data to this which
                                                                    	
                                                                    	mHexChosen = null;
                                                                    	mBuildingChosen = null;
                                                                        mScreenOn =
                                                                                Screen.MAP_SCREEN;
                                                                    }));
                                                });
                                    }

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
        // updateTokens(deltaTime);  TODO Move to server

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
        if (GameActions.LEFT_CLICK.isActivated()
                && UIManager.getInstance().getHoveredObject() == null) {
            Vector2d cursorPosition = GameActions.getCursor().getPosition();
            // TODO Check which tile has been selected Auri has said he will convert from screen to
            // local.

            mHexChosen = null; // TODO Work out which one chosen

            if (mHexChosen != null) {
                Reference<Building> buildingOnTile =
                        new Reference<Building>(
                                mPlayer.get()
                                        .getMapComponent()
                                        .get()
                                        .getBuilding(mHexChosen.getQ(), mHexChosen.getR()));
                if (buildingOnTile == null) {
                    // TODO Check if it can place a building there

                } else if (hasPlayerGotBuilding(buildingOnTile)) {
                    mBuildingChosen = buildingOnTile.get();
                    mScreenOn = Screen.BUILDING_SCREEN;
                } else {
                    return;
                }
            }
            // Check to see whether the user has pressed a tile.  And then send that to server

            // TODO CLIENT SIDE

        }
    }

    private boolean hasPlayerGotBuilding(Reference<Building> buildingToCheck) {
        for (int i = 0; i < mPlayer.get().numberOfBuildings(); i++) {
            Reference<Building> building = mPlayer.get().getBuilding(i);

            if (building == buildingToCheck) {
                return true;
            }
        }
        return false;
    }
}
