package org.dragonskulle.game.player;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UIMenuLeftDrawer extends Component implements IFrameUpdate, IOnStart {
    private final List<UITextButtonFrame> mButtonChildren = new ArrayList<>();

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
        mButtonChildren.add(new UITextButtonFrame("Button 1", (button, __) -> {
        }));
        mButtonChildren.add(new UITextButtonFrame("Button 2", (button, __) -> {
        }));
        mButtonChildren.add(new UITextButtonFrame("Button 3", (button, __) -> {
        }));
        mButtonChildren.add(new UITextButtonFrame("Button 4", (button, __) -> {
        }));

        final float offsetToTop = 0.46f;
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

        UIRenderable drawer = new UIRenderable(new SampledTexture("ui/drawer.png"));

        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setMargin(0f, 0f, 0f, 0f);
        tran.setPosition(-1.56f, 0f);
        getGameObject().addComponent(drawer);
    }

    public void updateButtons(List<UITextButtonFrame> newButtons) {
        this.mButtonChildren.clear();
        this.mButtonChildren.addAll(newButtons);
    }

//    UIVerticalSlider newSlider =
//            new UIVerticalSlider(
//                    (uiSlider, val) -> {
//                        if (scrollRef != null && scrollRef.isValid()) {
//                            scrollRef
//                                    .get()
//                                    .setTargetLerpTime(
//                                            (float)
//                                                    MathUtils.mapOneRangeToAnother(
//                                                            val, 0f, 100f, 0f, 1f, 2));
//                        }
//                    });
//
//    sliderReference = newSlider.getReference();
//        newSlider.setRoundStep(2f);
//        newSlider.setMaxValue(100f);
//        newSlider.setMinValue(0f);
//    getGameObject().addComponent(newSlider);
}
