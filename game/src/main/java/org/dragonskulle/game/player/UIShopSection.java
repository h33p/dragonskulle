/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/**
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class UIShopSection extends Component implements IOnStart {
    @Getter
    private ShopState mState = ShopState.BUILDING_SELECTED;
    private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    private UIBuildingOptions mBuildingOptions;
    private Reference<GameObject> mNewBuildingPanel;
    private Reference<GameObject> mUpgradePanel;

    @Setter
    @Getter
    private Reference<GameObject> mCurrentPanel = new Reference<>(null);


    public UIShopSection(UIMenuLeftDrawer.IGetPlayer mGetPlayer, UIMenuLeftDrawer.IGetHexChosen mGetHexChosen) {
        this.mGetPlayer = mGetPlayer;
        this.mGetHexChosen = mGetHexChosen;
    }

    public void setRandomState() {
        final ShopState[] values = ShopState.values();
        setState(values[(int) (Math.random() * values.length)]);
    }

    public enum ShopState {
        CLOSED,
        BUILDING_NEW,
        ATTACK_SCREEN,
        UPGRADE,
        BUILDING_SELECTED
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {
    }

    protected void setState(ShopState state) {
        Reference<GameObject> newPanel;
        log.warning("setting visible state to " + state.toString());
        switch (state) {
            case CLOSED:
                getGameObject().setEnabled(false);
                if (mCurrentPanel != null) {
                    mCurrentPanel.get().setEnabled(false);
                }
                return;
            case ATTACK_SCREEN:
                getGameObject().setEnabled(true);
                newPanel = new Reference<>(null);
                break;
            case BUILDING_NEW:
                getGameObject().setEnabled(true);
                newPanel = mNewBuildingPanel;
                break;
            case BUILDING_SELECTED: //todo is BUILDING_SELECTED the same as the upgrade panel?
            case UPGRADE:
                getGameObject().setEnabled(true);
                newPanel = mUpgradePanel;
                break;
            default:
                log.warning("Menu hasn't been updated to reflect this screen yet");
                setState(ShopState.CLOSED);
                return;
        }
        swapPanels(newPanel);
    }

    private void swapPanels(Reference<GameObject> newPanel) {
        log.warning("swapping panels");
        final boolean lastIsValid = mCurrentPanel.isValid();
        if (newPanel == null || !newPanel.isValid()) {
            log.warning("Swap panel case 1");
            if (mCurrentPanel != null && lastIsValid) {
                mCurrentPanel.get().setEnabled(false);
            }
            mCurrentPanel = newPanel;
        } else if (mCurrentPanel == null || !lastIsValid) {
            log.warning("Swap panel case 2");

            mCurrentPanel = newPanel;
            newPanel.get().setEnabled(true);
        } else if (!mCurrentPanel.equals(newPanel)) {
            log.warning("hiding the previous panel :: " + mCurrentPanel.get().getName());
            mCurrentPanel.get().setEnabled(false);
            mCurrentPanel = newPanel;
            if (newPanel.isValid()) {
                log.warning("showing panel :: " + newPanel.get().getName());
                newPanel.get().setEnabled(true);
            }
        }else{
            log.severe("none of the panel swaps caught me");
        }
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {

        mUpgradePanel = getGameObject().buildChild("building_upgrade_panel", new TransformUI(true),
                (self) -> {
                    UIBuildingUpgrade mUpgradeComponent = new UIBuildingUpgrade(mGetHexChosen);
                    self.addComponent(mUpgradeComponent);
                });
        mUpgradePanel.get().setEnabled(false);

        mNewBuildingPanel =
                getGameObject()
                        .buildChild(
                                "building_new_options",
                                new TransformUI(true),
                                (self) -> {
                                    mBuildingOptions = new UIBuildingOptions(mGetPlayer);
                                    self.addComponent(mBuildingOptions);
                                    TransformUI tran =
                                            getGameObject().getTransform(TransformUI.class);
                                    tran.setParentAnchor(0.08f, 0.8f, 1 - 0.08f, 1 - 0.04f);
                                });
        mNewBuildingPanel.get().setEnabled(false);
        //Outer window stuff
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setParentAnchor(0.08f, 0.73f, 1 - 0.08f, 1 - 0.03f);
        getGameObject().addComponent(new UIRenderable(new SampledTexture("white.bmp")));
        Reference<GameObject> textObj =
                getGameObject()
                        .buildChild(
                                "main_shop_text",
                                new TransformUI(true),
                                (self) -> {
                                    UIText mWindowText =
                                            new UIText(
                                                    new Vector3f(0f, 0f, 0f),
                                                    Font.getFontResource("Rise of Kingdom.ttf"),
                                                    "PLACEHOLDER SHOP TEXT");
                                    self.addComponent(mWindowText);
                                });

        TransformUI textTransform = textObj.get().getTransform(TransformUI.class);
        textTransform.setParentAnchor(0.05f, 0f);
        textTransform.translate(0, -0.18f);
    }
}
