/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UISlider;
import org.dragonskulle.utils.MathUtils;

/** @author Oscar L */
public class UILinkedScrollBar extends Component implements IFrameUpdate, IOnStart {
    private Reference<ScrollTranslate> scrollRef;
    private Reference<Component> sliderReference;

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {
        if (Reference.isValid(sliderReference)) {
            ((UISlider) sliderReference.get())
                    .setValue(
                            (float)
                                    MathUtils.mapOneRangeToAnother(
                                            scrollRef.get().getZoomLevel(), 0, 1, 0, 100, 0));
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
        scrollRef =
                Scene.getActiveScene()
                        .getSingleton(Camera.class)
                        .getGameObject()
                        .getComponent(ScrollTranslate.class);
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        tran.setParentAnchor(0.95f, 0.01f, 0.95f, 0.01f);
        tran.setPivotOffset(0f, 1f);
        tran.setMargin(0f, -0.2f, 0.2f, 0f);
        tran.setRotationDeg(90f);

        UISlider newSlider =
                new UISlider(
                        (uiSlider, val) -> {
                            if (Reference.isValid(scrollRef)) {
                                scrollRef
                                        .get()
                                        .setTargetLerpTime(
                                                (float)
                                                        MathUtils.mapOneRangeToAnother(
                                                                val, 0f, 100f, 0f, 1f, 2));
                            }
                        });

        sliderReference = newSlider.getReference();
        newSlider.setRoundStep(1f);
        newSlider.setMaxValue(100f);
        newSlider.setMinValue(0f);
        getGameObject().addComponent(newSlider);
    }
}
