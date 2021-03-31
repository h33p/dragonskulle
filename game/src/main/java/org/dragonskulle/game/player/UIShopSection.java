package org.dragonskulle.game.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
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
    private ShopState mState = ShopState.CLOSED;
    private Reference<UIText> mWindowTextRef;

    public void setRandomState() {
        final ShopState[] values = ShopState.values();
        setState(values[(int) (Math.random() * values.length)]);
    }

    public enum ShopState {CLOSED, BUILDING_NEW, BUILDING_SELECTED}


    protected void setState(ShopState state) {
        log.info("Setting shop state to " + state);
        this.mState = state;
        switch (mState) {
            case CLOSED:
                this.setEnabled(false);
                break;
            case BUILDING_NEW:
                if (mWindowTextRef != null && mWindowTextRef.isValid()) {
                    mWindowTextRef.get().setText("NEW BUILDING SHOP");
                }
                this.setEnabled(true);
                break;
            case BUILDING_SELECTED:
                if (mWindowTextRef != null && mWindowTextRef.isValid()) {
                    mWindowTextRef.get().setText("BUILDING SELECTED SHOP");
                }
                this.setEnabled(true);
                break;
        }
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {

    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        UIRenderable mWindow = new UIRenderable(new SampledTexture("ui/hor_drawer.png"));
        UIText mWindowText = new UIText(new Vector3f(0f, 0f, 0f),
                Font.getFontResource(
                        "Rise of Kingdom.ttf"),
                "PLACEHOLDER SHOP TEXT");
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
//        tran.setMargin(0f, 0.2f, 0f, -0.2f);
        tran.setParentAnchor(0f, 0f);
        tran.setPivotOffset(0f, 0f);
        getGameObject().addComponent(mWindow);
        getGameObject().addComponent(mWindowText);

        mWindowTextRef = getGameObject().getComponent(UIText.class);
    }
}
