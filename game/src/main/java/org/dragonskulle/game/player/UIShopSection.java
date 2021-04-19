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
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIMaterial;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;

/** @author Oscar L */
@Accessors(prefix = "m")
@Log
public class UIShopSection extends Component implements IOnStart {
    @Getter private ShopState mState = ShopState.MY_BUILDING_SELECTED;
    @Setter @Getter private ShopState mLastState = ShopState.CLOSED;
    private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    private Reference<GameObject> mNewBuildingPanel;
    private Reference<GameObject> mUpgradePanel;

    @Setter @Getter private Reference<GameObject> mCurrentPanel = new Reference<>(null);
    private Reference<UIText> titleRef;

    public UIShopSection(
            UIMenuLeftDrawer.IGetPlayer mGetPlayer, UIMenuLeftDrawer.IGetHexChosen mGetHexChosen) {
        this.mGetPlayer = mGetPlayer;
        this.mGetHexChosen = mGetHexChosen;
    }

    public enum ShopState {
        CLOSED,
        BUILDING_NEW,
        ATTACK_SCREEN,
        MY_BUILDING_SELECTED
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    @Override
    protected void onDestroy() {}

    protected void setState(ShopState state) {
        if (state != getLastState()) {
            if (state.equals(ShopState.CLOSED)) {
                show(mNewBuildingPanel, false);
                show(mCurrentPanel, false);
                setLastState(state);
                swapPanels(new Reference<>(null));
                return;
            }
            Reference<GameObject> newPanel;
            String newText = "Shop is Closed";
            log.warning("setting visible state to " + state);
            switch (state) {
                case ATTACK_SCREEN:
                    newPanel = new Reference<>(null);
                    break;
                case BUILDING_NEW:
                    //                    show(mNewBuildingPanel, true);
                    //                    newPanel = mNewBuildingPanel;
                    newPanel = new Reference<>(null);
                    break;
                case MY_BUILDING_SELECTED:
                    newPanel = mUpgradePanel;
                    newText = "Upgrade Building";
                    break;
                default:
                    log.warning("Menu hasn't been updated to reflect this screen yet  " + state);
                    setState(ShopState.CLOSED);
                    return;
            }

            if (Reference.isValid(titleRef)) {
                titleRef.get().setText(newText);
            }
            setLastState(state);
            swapPanels(newPanel);
        }
    }

    private void show(Reference<GameObject> component, boolean shouldShow) {
        if (Reference.isValid(component)) {
            component.get().setEnabled(shouldShow);
        }
    }

    private void swapPanels(Reference<GameObject> newPanel) {
        log.info("swapping panels");
        // if there is a screen being shown
        // deactivate the panel
        if (Reference.isValid(mCurrentPanel)) {
            if (mCurrentPanel.equals(mUpgradePanel)) {
                mUpgradePanel
                        .get()
                        .getComponent(UIBuildingUpgrade.class)
                        .get()
                        .setLastBuilding(null);
            }
            mCurrentPanel.get().setEnabled(false);
        }

        log.info("maybe showing newPanel");
        if (Reference.isValid(newPanel)) {
            newPanel.get().setEnabled(true);
        }
        mCurrentPanel = newPanel;
        //        show(mCurrentPanel, false);
        //        mCurrentPanel = activateNewPanel(newPanel);// activate the new panel and assign
        // the last current variable
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        UIBuildingUpgrade uiBuildingUpgrade = new UIBuildingUpgrade(mGetHexChosen, mGetPlayer);
        mUpgradePanel =
                getGameObject()
                        .buildChild(
                                "upgrade_object",
                                new TransformUI(true),
                                (self) -> self.addComponent(uiBuildingUpgrade));
        show(mUpgradePanel, false);

        UIBuildingOptions uiBuildingOptions = new UIBuildingOptions(mGetPlayer);
        mNewBuildingPanel =
                getGameObject()
                        .buildChild(
                                "building_object",
                                new TransformUI(true),
                                (self) -> self.addComponent(uiBuildingOptions));
        show(mNewBuildingPanel, false);

        // Outer window stuff
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setParentAnchor(0.08f, 0.68f, 1 - 0.08f, 1 - 0.03f);
        UIRenderable renderable = new UIRenderable(new SampledTexture("white.bmp"));
        ((UIMaterial) renderable.getMaterial()).colour.set(0.235, 0.219, 0.235, 1);
        getGameObject().addComponent(renderable);
        Reference<GameObject> textObj =
                getGameObject()
                        .buildChild(
                                "main_shop_text",
                                new TransformUI(true),
                                (self) -> {
                                    UIText mWindowText = new UIText("Shop is Closed");
                                    self.addComponent(mWindowText);
                                    titleRef = mWindowText.getReference(UIText.class);
                                });

        TransformUI textTransform = textObj.get().getTransform(TransformUI.class);
        textTransform.setParentAnchor(0.05f, 0f);
        textTransform.translate(0, -0.22f);
    }
}
