package org.dragonskulle.game.player;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.joml.Vector2f;

/**
 * @author Oscar L
 */
@Log
public class FancyCursor extends Component implements IOnStart, IFrameUpdate {

    private TransformUI mCursorTransform;
    private UIRenderable mFancyCursor;
    private float mManualXOffset = 2f;

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
        final Vector2f position = UIManager.getInstance().getScaledCursorCoords();
        if (mCursorTransform != null) {
            mCursorTransform.setPosition(position.x, position.y);
        }
//
//        if(GameActions.LEFT_CLICK.isActivated()){
//            mFancyCursor.
//        }
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mFancyCursor = new UIRenderable(new SampledTexture("ui/cursor.png"));
        getGameObject().addComponent(mFancyCursor);
        mCursorTransform = getGameObject().getTransform(TransformUI.class);
        mCursorTransform.setParentAnchor(0.01f, 0f, 0.01f, 1f);
        mCursorTransform.setMargin(0.01f, -0.01f, -0.01f, 0.01f);
    }
}
