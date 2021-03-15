package org.dragonskulle.game.player;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UITransform;
import org.joml.Vector2d;
import org.joml.Vector4f;


/**
 * This class will allow a user to interact with game.
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
public class HumanPlayer extends Component implements IFrameUpdate, IOnStart {

    private Screen screenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mapScreen;
    private Reference<GameObject> placeScreen;
    private Reference<GameObject> buildingScreen;
    private Reference<GameObject> chooseAttack;
    private Reference<GameObject> showStat;

    private Reference<Player> player;


    private HexagonTile hexChosen;
    private IAmNotABuilding buildingChosen;

    /**
     * The constructor for the human player
     *
     * @param map     the map being used for this game
     * @param capital the capital used by the player
     */
    public HumanPlayer() {
    }

    @Override
    public void onStart() {
        // TODO Auto-generated method stub

        mapScreen = getGameObject().buildChild("map screen", new UITransform(), (go) -> {
            go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
            //TODO How to click hex

        });  //This will draw a rectangle to the screen.  Need way to change screen

        placeScreen = getGameObject().buildChild("place screen", new UITransform(), (go) -> {
            go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
            go.buildChild("confirm box", new UITransform(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {
                    //TODO When clicked send to server (via calling Player) to try & build thing and then turn back to mapScreen

                    screenOn = Screen.MAP_SCREEN;
                }));
            });
            go.buildChild("Go Back", new UITransform(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {

                    screenOn = Screen.MAP_SCREEN;
                }));
            });
        });

        buildingScreen = getGameObject().buildChild("building options", new UITransform(), (go) -> {
            go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));
            go.buildChild("Upgrade Button", new UITransform(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));  //Make way to Go back
                box.addComponent(new UIButton((handle, __) -> {
                    //TODO When clicked need to show options to upgrade building stats
                    screenOn = Screen.STAT_SCREEN;
                }));
            });
            go.buildChild("Attack building", new UITransform(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {
                    //TODO When clicked need to show buildings which can be attacked -- get off building
                    screenOn = Screen.ATTACK_SCREEN;
                }));
            });
            go.buildChild("Sell building", new UITransform(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {
                    //TODO When clicked need to sell building
                    screenOn = Screen.MAP_SCREEN;
                }));
            });
            go.buildChild("Go Back", new UITransform(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {

                    screenOn = Screen.MAP_SCREEN;
                }));
            });
        });

        chooseAttack = getGameObject().buildChild("attack screen", new TransformUI(), (go) -> {
            go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

            for (IAmNotABuilding building : buildingChosen.attackableBuildings()) {
                go.buildChild("Attack building", new TransformUI(true), (box) -> {
                    box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                    box.addComponent(new UIButton((handle, __) -> {
                        //TODO When clicked need to attack building
                        screenOn = Screen.MAP_SCREEN;
                    }));
                });
            }

            go.buildChild("Go Back", new TransformUI(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {

                    screenOn = Screen.MAP_SCREEN;
                }));
            });
        });

        showStat = getGameObject().buildChild("Stat screen", new TransformUI(), (go) -> {
            go.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.3f)));

            ;    //TODO will add stuff for Stats AFTER prototype

            go.buildChild("Go Back", new TransformUI(true), (box) -> {
                box.addComponent(new UIRenderable(new SampledTexture("ui/wide_button.png")));
                box.addComponent(new UIButton((handle, __) -> {

                    screenOn = Screen.MAP_SCREEN;
                }));
            });
        });


    }

    @Override
    protected void onDestroy() {

    }

    @Override
    public void frameUpdate(float deltaTime) {
        //updateTokens(deltaTime);  TODO Move to server

        mapScreen.get().setEnabled(screenOn == Screen.MAP_SCREEN);
        placeScreen.get().setEnabled(screenOn == Screen.TILE_SCREEN);
        buildingScreen.get().setEnabled(screenOn == Screen.BUILDING_SCREEN);
        chooseAttack.get().setEnabled(screenOn == Screen.ATTACK_SCREEN);
        showStat.get().setEnabled(screenOn == Screen.STAT_SCREEN);
        if (screenOn == Screen.MAP_SCREEN) {
            mapScreen();
        }
    }

    /**
     * This will choose what to do when the user can see the full map
     */
    private void mapScreen() {
        if (GameActions.LEFT_CLICK.isActivated() && UIManager.getInstance().getHoveredObject() == null) {
            Vector2d cursorPosition = GameActions.getCursor().getPosition();
            //TODO Check which tile has been selected Auri has said he will convert from screen to local.

            hexChosen = null;  //TODO Work out which one chosen


            if (hexChosen != null) {
                IAmNotABuilding buildingOnTile = HexagonMap.getBuilding(hexChosen.getMR(), hexChosen.getMQ());
                if (buildingOnTile == null) {
                    // TODO Check if it can place a building there

                } else if (hasPlayerGotBuilding(buildingOnTile)) {
                    buildingChosen = buildingOnTile;
                    screenOn = Screen.BUILDING_SCREEN;
                } else {
                    return;
                }
            }
            // Check to see whether the user has pressed a tile.  And then send that to server

            //TODO CLIENT SIDE

        }

    }

    private boolean hasPlayerGotBuilding(IAmNotABuilding buildingToCheck) {
        for (int i = 0; i < player.get().numberOfBuildings(); i++) {
            IAmNotABuilding building = player.get().getBuilding(i);

            if (building == buildingToCheck) {
                return true;
            }
        }
        return false;

    }


}