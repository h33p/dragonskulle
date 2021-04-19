/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.UIManager;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * Allows to control object with mouse dragging.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class DragMovement extends Component implements IFrameUpdate, IOnAwake {

    private transient Transform3D mTransform;

    private Reference<HexagonMap> mMap;
    private Reference<Transform> mTargetPlane;
    private float mTargetHeight = 0f;

    private final Vector3f mPlanePos = new Vector3f();
    private final Vector3f mTmpPos = new Vector3f();
    private final Vector3f mTmpPosX = new Vector3f();
    private final Vector3f mTmpPosY = new Vector3f();

    private boolean mDragging;

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {

        if (mTransform == null) return;

        if (!Reference.isValid(mMap)) {
            HexagonMap map = Scene.getActiveScene().getSingleton(HexagonMap.class);
            if (map != null) mMap = map.getReference(HexagonMap.class);
            else return;
        }

        if (!Reference.isValid(mTargetPlane)) {
            mTargetPlane = mMap.get().getGameObject().getTransform().getReference(Transform.class);
        }

        if (!Reference.isValid(mTargetPlane)) return;

        Transform target = mTargetPlane.get();

        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);

        if (mainCam == null) return;

        Cursor cursor = Actions.getCursor();

        if (cursor == null) return;

        Vector2fc screenPos = cursor.getPosition();

        HexagonTile tile = mMap.get().cursorToTile();

        float height = tile != null ? tile.getHeight() : 0;

        Vector3f pos =
                mainCam.screenToPlane(
                        target,
                        mDragging ? mTargetHeight : height,
                        screenPos.x(),
                        screenPos.y(),
                        mTmpPos);

        if (!mDragging
                && cursor.hadLittleDrag()
                && !Reference.isValid(UIManager.getInstance().getHoveredObject())) {
            mPlanePos.set(pos);
            mTargetHeight = height;
            mDragging = true;
        } else if (!Actions.TRIGGER_DRAG.isActivated()) {
            mDragging = false;
        }

        if (!mDragging) return;

        // Here we need to figure out how to place mPlanePos under the cursor

        // Let's just figure out by deltas. This is extremely inefficient, but hey, it works!
        mTransform.translate(1, 0, 0);
        Vector3f dX =
                mainCam.screenToPlane(
                        target, mTargetHeight, screenPos.x(), screenPos.y(), mTmpPosX);
        dX.sub(pos);

        mTransform.translate(-1, 1, 0);
        Vector3f dY =
                mainCam.screenToPlane(
                        target, mTargetHeight, screenPos.x(), screenPos.y(), mTmpPosY);
        dY.sub(pos);

        mTransform.translate(0, -1, 0);

        // Shift back so we know the delta we are looking for
        pos.sub(mPlanePos).negate();

        // Now we just need to solve for x * dX + y * dY = pos:
        //
        // x * dX.x + y * dY.x = pos.x
        // x * dX.y + y * dY.y = pos.y
        //
        // x * dX.x * dX.y + y * dY.x * dX.y = pos.x * dX.y
        // x * dX.y * dX.x + y * dY.y * dX.x = pos.y * dX.x
        //
        // y * (dY.x * dX.y - dY.y * dX.x) = pos.x * dX.y - pos.y * dX.x
        float y = (pos.x * dX.y - pos.y * dX.x) / (dY.x * dX.y - dY.y * dX.x);

        // x * dX.x + y * dY.x = pos.x
        // x * dX.x = pos.x - y * dY.x
        float x = (pos.x - y * dY.x) / dX.x;

        mTransform.translate(x, y, 0);
    }

    @Override
    protected void onDestroy() {}
}
