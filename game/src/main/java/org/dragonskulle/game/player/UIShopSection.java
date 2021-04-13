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
    private ShopState mState = ShopState.MY_BUILDING_SELECTED;
    @Setter
    @Getter
    private ShopState mLastState = ShopState.CLOSED;
    private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    private UIBuildingOptions mBuildingOptions;
    private Reference<GameObject> mNewBuildingPanel;
    private Reference<GameObject> mUpgradePanel;

    @Setter
    @Getter
    private Reference<GameObject> mCurrentPanel = new Reference<>(null);
    private Reference<UIText> titleRef;

    public UIShopSection(
            UIMenuLeftDrawer.IGetPlayer mGetPlayer, UIMenuLeftDrawer.IGetHexChosen mGetHexChosen) {
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
        MY_BUILDING_SELECTED
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {
    }

    protected void setState(ShopState state) {
        if (state != getLastState()) {
            if (state.equals(ShopState.CLOSED)) {
                show(false);
                if (Reference.isValid(mCurrentPanel)) {
                    show(mCurrentPanel, false);
                }
                setLastState(state);
                swapPanels(new Reference<>(null));
                return;
            }
            Reference<GameObject> newPanel;
            log.warning("setting visible state to " + state);
            switch (state) {
                case ATTACK_SCREEN:
                    show(true);
                    newPanel = new Reference<>(null);
                    break;
                case BUILDING_NEW:
                    show(true);
                    newPanel = mNewBuildingPanel;
                    break;
                case MY_BUILDING_SELECTED: // todo is BUILDING_SELECTED the same as the upgrade panel?
                    show(true);
                    newPanel = mUpgradePanel;
                    break;
                default:
                    log.warning("Menu hasn't been updated to reflect this screen yet  " + state);
                    setState(ShopState.CLOSED);
                    return;
            }
            if (Reference.isValid(titleRef)) {
                titleRef.get().setText(state.toString());
            }
            setLastState(state);
            swapPanels(newPanel);
        }
    }

    private void show(boolean shouldShow) {
        getGameObject().setEnabled(shouldShow);
    }

    private void show(Reference<GameObject> gameObject, boolean show) {
        gameObject.get().setEnabled(show);
    }

    private void swapPanels(Reference<GameObject> newPanel) {
        if (Reference.isValid(mCurrentPanel)) {
            // there is a screen being shown
            // deactivate the panel
            show(mCurrentPanel, false);
        }
        // activate the new panel and assign the last current variable
        mCurrentPanel = activateNewPanel(newPanel);
    }

    private Reference<GameObject> activateNewPanel(Reference<GameObject> newPanel) {
        // check if valid reference then reassign
        if (Reference.isValid(newPanel)) {
            show(newPanel, true);
            return newPanel;
        }
        return new Reference<>(null);
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {

        mUpgradePanel =
                getGameObject()
                        .buildChild(
                                "building_upgrade_panel",
                                new TransformUI(true),
                                (self) -> {
                                    UIBuildingUpgrade mUpgradeComponent =
                                            new UIBuildingUpgrade(mGetHexChosen);
                                    self.addComponent(mUpgradeComponent);
                                });
        show(mUpgradePanel, false);

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
        show(mNewBuildingPanel, false);
        // Outer window stuff
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setParentAnchor(0.08f, 0.68f, 1 - 0.08f, 1 - 0.03f);
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

                                    titleRef = mWindowText.getReference(UIText.class);
                                });

        TransformUI textTransform = textObj.get().getTransform(TransformUI.class);
        textTransform.setParentAnchor(0.05f, 0f);
        textTransform.translate(0, -0.22f);
    }
}
