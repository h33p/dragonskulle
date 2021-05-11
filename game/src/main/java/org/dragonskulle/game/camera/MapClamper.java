/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.map.HexagonMap;
import org.joml.Vector3f;

/**
 * Clamps camera rig to the hexagon map bounds.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class MapClamper extends Component implements IFrameUpdate, IOnStart, ILateFrameUpdate {

    private transient Transform3D mTransform;

    @Getter private final Vector3f mBBMin = new Vector3f(Float.POSITIVE_INFINITY);
    @Getter private final Vector3f mBBMax = new Vector3f(Float.NEGATIVE_INFINITY);

    private Reference<HexagonMap> mMap;

    private final Vector3f mStartPos = new Vector3f();

    @Override
    public void onStart() {
        mTransform = getGameObject().getTransform(Transform3D.class);
        mMap = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mTransform == null) {
            return;
        }

        mTransform.getPosition(mStartPos);
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        if (mTransform == null || !Reference.isValid(mMap)) {
            mMap = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
            return;
        }

        mMap.get().calculateVisibleTileBounds(mBBMin, mBBMax);

        // Extracted from Transform as to not modify the normal transform code
        Vector3f pos = new Vector3f(mStartPos);
        Vector3f mTmpPos = new Vector3f();
        Vector3f mTmpDir = new Vector3f();

        mTransform.getPosition(mTmpPos);

        float length = mTmpPos.distance(pos);

        // Clamp target position to be within the bounding box
        if (mBBMax.x >= mBBMin.x) {
            mTmpPos.min(new Vector3f(mBBMax.x, mBBMax.y, mTmpPos.z));
            mTmpPos.max(new Vector3f(mBBMin.x, mBBMin.y, mTmpPos.z));
        }

        // Move up to length towards the clamped position from pos
        mTmpDir.set(pos).sub(mTmpPos).negate();

        float lengthToClamped = mTmpDir.length();

        float targetLength = Math.min(length, lengthToClamped);

        if (targetLength != 0) {
            pos.add(mTmpDir.normalize(targetLength));
        }

        mTransform.setPosition(pos);
    }

    @Override
    protected void onDestroy() {}
}
