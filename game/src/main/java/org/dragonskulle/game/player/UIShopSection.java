/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
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
        UIText mWindowText =
                new UIText(
                        new Vector3f(0f, 0f, 0f),
                        Font.getFontResource("Rise of Kingdom.ttf"),
                        "PLACEHOLDER SHOP TEXT");
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        //        tran.setMargin(0f, 0.2f, 0f, -0.2f);
        tran.setParentAnchor(0.08f, 0.73f, 1 - 0.08f, 1 - 0.03f);
        getGameObject().addComponent(mWindow);
        getGameObject().addComponent(mWindowText);

        mWindowTextRef = getGameObject().getComponent(UIText.class);
    }
}
