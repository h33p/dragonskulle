/* (C) 2021 DragonSkulle */
package org.dragonskulle.devtools;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.input.Action;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.renderer.Renderer;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Render debug text display
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class RenderDebug extends Component implements IOnAwake, IFrameUpdate {

    public static final Action DEBUG_ACTION = new Action();

    private Reference<UIText> mText;

    @Getter @Setter private Reference<Transform> mTrackTransform;

    private boolean mLastPressed = false;

    @Override
    public void onAwake() {
        mText = getGameObject().getComponent(UIText.class);

        if (mText == null) {
            UIText text = new UIText();
            getGameObject().addComponent(text);
            mText = text.getReference(UIText.class);
        }

        mText.get().setEnabled(false);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        boolean debugPressed = DEBUG_ACTION.isActivated();

        if (mText != null && mText.isValid()) {
            if (debugPressed && !mLastPressed) mText.get().setEnabled(!mText.get().isEnabled());

            mLastPressed = debugPressed;

            if (!mText.get().isEnabled()) return;

            Renderer rend = Engine.getInstance().getGLFWState().getRenderer();

            int instancedDrawCalls = rend.getInstancedCalls();
            int slowDrawCalls = rend.getSlowCalls();

            String fps = String.format("FPS: %.2f (%.1f ms)\n", 1f / deltaTime, deltaTime * 1000f);

            final String cameraText;

            Camera cam = Scene.getActiveScene().getSingleton(Camera.class);

            if (cam != null) {
                Transform tr = cam.getGameObject().getTransform();
                Vector3f pos = tr.getPosition();
                Vector3f rot = tr.getWorldMatrix().getEulerAnglesZYX(new Vector3f());
                cameraText =
                        String.format(
                                "X: %.2f Y: %.2f Z: %.2f\nP: %.2f R: %.2f Y: %.2f\n",
                                pos.x,
                                pos.y,
                                pos.z,
                                rot.x / MathUtils.DEG_TO_RAD,
                                rot.y / MathUtils.DEG_TO_RAD,
                                rot.z / MathUtils.DEG_TO_RAD);
            } else {
                cameraText = "";
            }

            Vector2f scaledCursorCoords = UIManager.getInstance().getScaledCursorCoords();

            Cursor cursor = Actions.getCursor();

            final String cursorText;

            if (cursor != null) {
                Vector2d cursorCoords = cursor.getPosition();
                cursorText =
                        String.format(
                                "%.2f %.2f (%.0f %.0f)\n",
                                scaledCursorCoords.x,
                                scaledCursorCoords.y,
                                cursorCoords.x,
                                cursorCoords.y);
            } else {
                cursorText =
                        String.format("%.2f %.2f\n", scaledCursorCoords.x, scaledCursorCoords.y);
            }

            String rendererText =
                    String.format(
                            "Instanced Draws: %d\nSlow Draws: %d\nIB Size: %d\nMB Size: VB - %d | IB - %d\n",
                            instancedDrawCalls,
                            slowDrawCalls,
                            rend.getInstancedCalls(),
                            rend.getVertexBufferSize(),
                            rend.getIndexBufferSize());

            mText.get().setText(fps + cameraText + cursorText + rendererText);
        }
    }

    @Override
    public void onDestroy() {}
}
