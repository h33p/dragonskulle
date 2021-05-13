/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * Stick an object to the map tile.
 *
 * <p>This works only, and only if the object does not move after spawning.
 *
 * @author Aurimas Blažulionis
 */
public class StickToTile extends Component implements IFrameUpdate {

    /** Store the {@link HexagonMap} that the object is on. */
    private Reference<HexagonMap> mMap = null;

    /** Cached position on the map. */
    private Vector3i mPosition = null;

    @Override
    public void frameUpdate(float deltaTime) {
        TransformHex hexTransform = getGameObject().getTransform(TransformHex.class);
        HexagonTile tile = getTile();

        if (hexTransform != null && tile != null) {
            hexTransform.setHeight(tile.getSurfaceHeight());
        }
    }

    /**
     * Get the {@link HexagonTile} the {@link Building} is on.
     *
     * @return The tile the building is on, or {@code null}.
     */
    public HexagonTile getTile() {
        HexagonMap map = getMap();
        if (map == null) {
            return null;
        }

        Vector3i position = getPosition();

        if (position != null) {
            HexagonTile tile = map.getTile(position.x(), position.y());
            mPosition = position;
            return tile;
        }

        return null;
    }

    /**
     * Get the current axial coordinates of the building using the {@link GameObject}'s {@link
     * TransformHex}.
     *
     * @return A 3d-vector of integers containing the x, y and z position of the building, or {@code
     *     null}.
     */
    private Vector3i getPosition() {
        if (mPosition != null) {
            return mPosition;
        }

        TransformHex tranform = getGameObject().getTransform(TransformHex.class);

        if (tranform == null) {
            return null;
        }

        Vector3f floatPosition = new Vector3f();
        tranform.getLocalPosition(floatPosition);

        Vector3i integerPosition = new Vector3i();
        integerPosition.set(
                (int) floatPosition.x(), (int) floatPosition.y(), (int) floatPosition.z());

        return integerPosition;
    }

    /**
     * Get the {@link HexagonMap} being used.
     *
     * @return The map, if it exists, or {@code null}.
     */
    private HexagonMap getMap() {
        if (!Reference.isValid(mMap)) {
            mMap = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
        }

        return Reference.isValid(mMap) ? mMap.get() : null;
    }

    @Override
    protected void onDestroy() {}
}
