/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
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

/** @author Oscar L */
@Accessors(prefix = "m")
@Log
public class UIShopSection extends Component implements IOnStart {
    @Getter private ShopState mState = ShopState.BUILDING_SELECTED;
    private Reference<UIText> mWindowTextRef;
    private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private UIBuildingOptions mBuildingOptions;

    public UIShopSection(UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetPlayer = mGetPlayer;
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

    protected void setState(ShopState state) {
        log.info("Setting shop state to " + state);
        this.mState = state;
        switch (mState) {
            case CLOSED:
                getGameObject().setEnabled(false);
                break;
            case BUILDING_NEW:
                mBuildingOptions.setEnabled(true);
                if (mWindowTextRef != null && mWindowTextRef.isValid()) {
                    mWindowTextRef.get().setText("NEW BUILDING SHOP");
                }
                getGameObject().setEnabled(true);
                break;
            case ATTACK_SCREEN:
                if (mWindowTextRef != null && mWindowTextRef.isValid()) {
                    mWindowTextRef.get().setText("ATTACK SHOP");
                }
                getGameObject().setEnabled(true);
                break;
            case UPGRADE:
                if (mWindowTextRef != null && mWindowTextRef.isValid()) {
                    mWindowTextRef.get().setText("UPGRADE SELECTED SHOP");
                }
                break;
            case BUILDING_SELECTED:
                if (mWindowTextRef != null && mWindowTextRef.isValid()) {
                    mWindowTextRef.get().setText("BUILDING SELECTED SHOP");
                }
                getGameObject().setEnabled(true);
                break;
        }
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        UIRenderable mWindow = new UIRenderable(new SampledTexture("white.bmp"));

        Reference<GameObject> buildingOptionsGO =
                getGameObject()
                        .buildChild(
                                "building_new_options",
                                new TransformUI(true),
                                (self) -> {
                                    mBuildingOptions = new UIBuildingOptions(mGetPlayer);
                                    mBuildingOptions.setEnabled(false);
                                    self.addComponent(mBuildingOptions);
                                    TransformUI tran =
                                            getGameObject().getTransform(TransformUI.class);
                                    tran.setParentAnchor(0.08f, 0.8f, 1 - 0.08f, 1 - 0.04f);
                                    ;
                                });

        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setParentAnchor(0.08f, 0.73f, 1 - 0.08f, 1 - 0.03f);
        getGameObject().addComponent(mWindow);
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
                                    mWindowTextRef = mWindowText.getReference(UIText.class);
                                });
        getGameObject().addComponent(mBuildingOptions);

        TransformUI textTransform = textObj.get().getTransform(TransformUI.class);
        textTransform.setParentAnchor(0.05f, 0f);
        textTransform.translate(0, -0.18f);
    }
}
