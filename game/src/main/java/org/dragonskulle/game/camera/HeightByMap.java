/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.camera.ScrollTranslate.IZoomNotify;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.utils.MathUtils;
import org.joml.Intersectionf;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Puts the object at offset height from the map height.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class HeightByMap extends Component implements IFrameUpdate, IOnAwake, IZoomNotify {
    @Getter @Setter private float mHeightOffset = 2f;
    @Getter @Setter private float mMinHeightLerped = 1f;
    @Getter @Setter private float mMaxZoomValue = 0.1f;
    @Getter @Setter private float mLerpSpeed = 5f;

    @Getter @Setter private float mZoomLevel;

    private Reference<HexagonMap> mMapReference = null;

    private transient Transform3D mTransform;

    private final Vector2f mAxial = new Vector2f();
    private final Vector3f mTransformed = new Vector3f();
    private final Vector3f mTmpTransformed = new Vector3f();
    private final List<HexagonTile> mTilesAround = new ArrayList<>();

    @Override
    public void onAwake() {
        mTransform = getGameObject().getTransform(Transform3D.class);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mTransform == null) {
            return;
        }

        if (!ensureMapReference()) {
            return;
        }

        mTransform.getLocalPosition(mTransformed);

        HexagonMap map = mMapReference.get();

        Matrix4fc worldMatrix = map.getGameObject().getTransform().getWorldMatrix();

        Matrix4fc invWorldMatrix = map.getGameObject().getTransform().getInvWorldMatrix();

        invWorldMatrix.transformPosition(mTransformed);

        TransformHex.cartesianToAxial(mTransformed, mAxial);

        TransformHex.roundAxial(mAxial, mTmpTransformed);

        HexagonTile t = map.getTile((int) mTmpTransformed.x, (int) mTmpTransformed.y);

        if (t == null) {
            return;
        }

        map.getTilesInRadius(t, 1, false, mTilesAround);

        float h = t.getHeight();

        if (mTilesAround.size() > 1) {
            mTilesAround.sort(
                    (ta, tb) ->
                            Float.compare(
                                    getTileDistance(mTransformed, ta),
                                    getTileDistance(mTransformed, tb)));

            final HexagonTile[] tiles = {t, mTilesAround.get(0), mTilesAround.get(1)};

            final float[] heights = new float[3];
            final Vector2f[] pos = new Vector2f[3];
            final Vector3f[] cart = new Vector3f[3];

            for (int i = 0; i < 3; i++) {
                heights[i] = tiles[i].getHeight();
                pos[i] = new Vector2f(tiles[i].getQ(), tiles[i].getR());
                cart[i] = TransformHex.axialToCartesian(pos[i], heights[i], new Vector3f());
            }

            Vector3f lerped = new Vector3f();

            Intersectionf.findClosestPointOnTriangle(
                    cart[0], cart[1], cart[2], mTransformed, lerped);

            h = lerped.z;
        }

        h = MathUtils.lerp(h, mMinHeightLerped, Math.min(1, mZoomLevel / mMaxZoomValue));

        mTransformed.z =
                MathUtils.lerp(
                        mTransformed.z, h + mHeightOffset, Math.min(1, deltaTime * mLerpSpeed));

        worldMatrix.transformPosition(mTransformed);

        mTransform.setPosition(mTransformed);
    }

    private float getTileDistance(Vector3fc point, HexagonTile tile) {
        mAxial.set(tile.getQ(), tile.getR());
        Vector3f vec = TransformHex.axialToCartesian(mAxial, point.z(), mTmpTransformed);
        return vec.distance(point);
    }

    @Override
    protected void onDestroy() {}

    private boolean ensureMapReference() {
        if (Reference.isValid(mMapReference)) {
            return true;
        }

        mMapReference = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
        return Reference.isValid(mMapReference);
    }
}
