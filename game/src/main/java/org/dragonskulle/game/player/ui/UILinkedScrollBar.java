/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UISlider;
import org.joml.Vector4f;

/**
 * A scroll bar that is linked to the mouse scroll position and map zoom.
 *
 * @author Oscar L
 */
public class UILinkedScrollBar extends Component implements IFrameUpdate, IOnStart {
    private Reference<ScrollTranslate> mScrollRef;
    private Reference<UISlider> mSliderReference;

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {
        if (Reference.isValid(mSliderReference)) {
            mSliderReference.get().setValue(mScrollRef.get().getTargetLerpTime());
        }
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mScrollRef =
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
                            if (Reference.isValid(mScrollRef)) {
                                mScrollRef.get().setTargetLerpTime(val);
                            }
                        });
        // Use a darker slider bar colour.
        newSlider.setColour(new Vector4f(0.4f));

        mSliderReference = newSlider.getReference(UISlider.class);
        newSlider.setRoundStep(0.01f);
        newSlider.setMaxValue(1f);
        newSlider.setMinValue(0f);
        getGameObject().addComponent(newSlider);
    }
}
