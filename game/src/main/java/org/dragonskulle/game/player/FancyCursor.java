/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIRenderable;
import org.joml.Vector2fc;

/**
 * Attaches a graphical component to wherever the cursor is, the cursor is hidden in {@code Cursor.java}.
 *
 * @author Oscar L
 */
@Log
public class FancyCursor extends Component implements IOnStart, IFrameUpdate {

    /**
     * The x offset to the 'point' of the cursor graphic.
     */
    private static final float X_SIZE_OFFSET_TO_POINT = 0.015f;
    /**
     * The y offset to the 'point' of the cursor graphic.
     */
    private static final float Y_SIZE_OFFSET_TO_POINT = 0.025f;
    /**
     * The cursor transform component.
     */
    private TransformUI mCursorTransform;
    /**
     * The renderable for the cursor.
     */
    private UIRenderable mFancyCursor;

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {}

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {
        final Vector2fc position = GameActions.getCursor().getPosition();
        float x = (position.x() + 1) * 0.5f + X_SIZE_OFFSET_TO_POINT;
        float y = (position.y() + 1) * 0.5f + Y_SIZE_OFFSET_TO_POINT;
        mCursorTransform.setParentAnchor(x, y, x, y);
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mFancyCursor = new UIRenderable(new SampledTexture("ui/cursor.png"));
        mFancyCursor.setDepthShift(Float.NEGATIVE_INFINITY);
        getGameObject().addComponent(mFancyCursor);
        mCursorTransform = getGameObject().getTransform(TransformUI.class);
        mCursorTransform.setMargin(-0.03f, -0.03f, 0.03f, 0.03f);
    }
}
