package org.dragonskulle.game.player;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UIMenuLeftDrawer extends Component implements IFrameUpdate, IOnStart {
    private Reference<GameObject> mPlaceScreen; //todo make this
    private Reference<GameObject> mBuildingSelectedScreen; //todo make this
    private Reference<GameObject> mShowStat; //todo make this
    private Reference<GameObject> mChooseAttack;
    private Reference<Building> mBuildingChosen;
    private final IGetHexChosen getHexChosen;
    private final ISetHexChosen setHexChosen;
    private final INotifyScreenChange notifyScreenChange;
    private final float offsetToTop = 0.46f;
    private Reference<Player> mPlayer;

    public interface INotifyScreenChange {
        void call(Screen newScreen);
    }

    public interface IGetHexChosen {
        HexagonTile get();
    }

    public interface ISetHexChosen {
        void set(HexagonTile tile);
    }

    public UIMenuLeftDrawer(Reference<Building> mBuildingChosen, IGetHexChosen getHexChosen, ISetHexChosen setHexChosen, INotifyScreenChange notifyScreenChange) {
        super();
        this.mBuildingChosen = mBuildingChosen;
        this.getHexChosen = getHexChosen;
        this.setHexChosen = setHexChosen;
        this.notifyScreenChange = notifyScreenChange;
    }

//    // Get the screen for confirming placing a buildingSelectedView
//    mPlaceScreen =
//
//    getGameObject()
//                        .
//
//    buildChild(
//                                "place screen",new TransformUI(), this::buildPlaceSelectedView);
//
//    // Screen to choose what to do for a buildingSelectedView
//    mBuildingSelectedScreen =
//
//    getGameObject()
//                        .
//
//    buildChild(
//                                "buildingSelectedView options",
//                                        new TransformUI(),
//                                this::buildBuildingSelectedView);
//
//    // To Attack
//    mChooseAttack =
//
//    getGameObject()
//                        .
//
//    buildChild("attackView screen",new TransformUI(), this::buildAttackView);
//
//    // To upgrade stats
//    mShowStat =
//
//    getGameObject()
//                        .
//
//    buildChild(
//                                "Stat screen",
//                                        new TransformUI(),
//                                (go)->
//
//    {
//        ; // TODO will add stuff for Stats AFTER prototype
//

//    });

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {

    }

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {

    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {

        //build the attack buildings menu dynamically
        ArrayList<UITextButtonFrame> attackBuildingsButton = new ArrayList<>();
        if (mBuildingChosen != null && mBuildingChosen.isValid()) {
            attackBuildingsButton.add(new UITextButtonFrame("Go Back", (handle, __) -> {
                setHexChosen.set(null);
                mBuildingChosen = null;
                notifyScreenChange.call(Screen.MAP_SCREEN);
            }));

            for (Building attackableBuilding : mBuildingChosen.get().getAttackableBuildings()) {
                attackBuildingsButton.add(
                        new UITextButtonFrame("Attack buildingSelectedView", (handle, __) -> {
                            // -- Need way to show different buildingSelectedView

                            // Send attackView to server
                            mPlayer.get()
                                    .getClientAttackRequest()
                                    .invoke(
                                            new AttackData(
                                                    mBuildingChosen.get(),
                                                    attackableBuilding)); // TODO Send
                            setHexChosen.set(null);
                            mBuildingChosen = null;
                            notifyScreenChange.call(Screen.MAP_SCREEN);
                        })
                );
            }
        }
        mChooseAttack = buildMenu(attackBuildingsButton);
        mChooseAttack.get().setEnabled(false);


        UIRenderable drawer = new UIRenderable(new SampledTexture("ui/drawer.png"));
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setMargin(0f, 0f, 0f, 0f);
        tran.setPosition(-1.56f, 0f);
        getGameObject().addComponent(drawer);
    }

    private Reference<GameObject> buildMenu(List<UITextButtonFrame> mButtonChildren) {
        return getGameObject().buildChild("auto_built_children", (menu) -> {
            for (int i = 0, mButtonChildrenSize = mButtonChildren.size(); i < mButtonChildrenSize; i++) {
                UITextButtonFrame mButtonChild = mButtonChildren.get(i);
                int finalI = i;
                getGameObject().buildChild("drawer_child_" + i, new TransformUI(true), (self) -> {
                    self.getTransform(TransformUI.class)
                            .setPosition(
                                    0f, (0.8f * finalI / mButtonChildrenSize * 1.3f) - offsetToTop);
                    self.getTransform(TransformUI.class)
                            .setMargin(0.075f, 0f, -0.075f, 0f);
                    self.addComponent(
                            new UIRenderable(
                                    new SampledTexture(
                                            "ui/wide_button_new.png")));
                    UIButton button = new UIButton(new UIText(
                            new Vector3f(0f, 0f, 0f),
                            Font.getFontResource("Rise of Kingdom.ttf"),
                            mButtonChild.getText()
                    ), mButtonChild.getOnClick());
                    self.addComponent(button);
                });

            }
        });
    }

    public void setMenu(Screen mScreenOn) {
        switch (mScreenOn) {
            case BUILDING_SELECTED_SCREEN:
                mBuildingSelectedScreen.get().setEnabled(true);
                break;
            case TILE_SCREEN:
                mPlaceScreen.get().setEnabled(true);
                break;
            case ATTACK_SCREEN:
                mChooseAttack.get().setEnabled(true);
                break;
            case STAT_SCREEN:
                mShowStat.get().setEnabled(true);
                break;
        }
    }

}
