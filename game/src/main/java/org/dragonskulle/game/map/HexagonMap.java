/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.ArrayList;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Scene;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.renderer.components.Camera;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Leela Muppala and Craig Wilbourne
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Accessors(prefix = "m")
@Log
public class HexagonMap extends Component implements IOnStart, IOnAwake {

    /** The size that is used to create the map. */
    @Getter private final int mSize;

    /** The map that is created which is made of a 2d array of HexagonTiles. */
    private HexagonTileStore mTiles;

    /**
     * HexagonMap constructor that gets the size for the map and calls the createHexMap function to
     * create the map.
     *
     * @param size the size of the map
     */
    public HexagonMap(int size) {
        this.mSize = size;

        if (size < 0) {
            throw new RuntimeException("The size should be greater than 0");
        }

        mTiles = new HexagonTileStore(mSize, 3);
    }

    /**
     * Get the {@link HexagonTile} at the specified position, or {@code null} if it doesn't exist.
     *
     * @param q The q coordinate.
     * @param r The r coordinate.
     * @return The HexagonTile, or {@code null}.
     */
    public HexagonTile getTile(int q, int r) {
        return mTiles.getTile(q, r);
    }

    /**
     * Get a stream of all hexagon tiles
     *
     * @return stream of all non-null hexagon tiles in the map
     */
    public Stream<HexagonTile> getAllTiles() {
        return mTiles.getAllTiles();
    }

    /**
     * Get all of the {@link HexagonTile}s in a radius around the selected tile. If {@code
     * includeTile} is {@code true}, the selected tile will be included in the list.
     *
     * @param tile The selected tile.
     * @param radius The radius around the selected tile.
     * @param includeTile Whether or not to include the selected tile in the resultant {@link
     *     ArrayList}.
     * @return A list of tiles within a radius of the selected tile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getTilesInRadius(
            HexagonTile tile, int radius, boolean includeTile) {
        return getTilesInRadius(tile.getQ(), tile.getR(), radius, includeTile);
    }

    public ArrayList<HexagonTile> getTilesInRadius(
            int q, int r, int radius, boolean includeCentre) {
        ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();

        // Get the tile's q and r coordinates.
        int qCentre = q;
        int rCentre = r;

        for (int rOffset = -radius; rOffset <= radius; rOffset++) {
            for (int qOffset = -radius; qOffset <= radius; qOffset++) {
                // Only get tiles whose s coordinates are within the desired range.
                int sOffset = -qOffset - rOffset;

                // Do not include tiles outside of the radius.
                if (sOffset > radius || sOffset < -radius) continue;
                // Do not include the building's HexagonTile.
                if (!includeCentre && qOffset == 0 && rOffset == 0) continue;

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = getTile(qCentre + qOffset, rCentre + rOffset);
                if (selectedTile == null) continue;

                // Add the tile to the list.
                tiles.add(selectedTile);
            }
        }

        return tiles;
    }

    /**
     * Get all of the {@link HexagonTile}s in a radius around the selected tile, not including the
     * tile.
     *
     * @param tile The selected tile.
     * @param radius The radius around the selected tile.
     * @return An {@link ArrayList} of tiles around, but not including, the selected tile; otherwise
     *     an empty ArrayList.
     */
    public ArrayList<HexagonTile> getTilesInRadius(HexagonTile tile, int radius) {
        return getTilesInRadius(tile, radius, false);
    }

    public HexagonTile cursorToTile() {
        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);

        if (mainCam == null) return null;

        // Retrieve scaled screen coordinates
        Cursor cursor = Actions.getCursor();

        if (cursor == null) return null;

        Vector2fc screenPos = cursor.getPosition();

        // Convert those coordinates to local coordinates within the map
        Vector3f pos =
                mainCam.screenToPlane(
                        getGameObject().getTransform(),
                        screenPos.x(),
                        screenPos.y(),
                        new Vector3f());

        Vector2f axial = new Vector2f();

        // Convert those coordinates to axial
        TransformHex.cartesianToAxial(pos, axial);
        // And round them
        TransformHex.roundAxial(axial, pos);

        HexagonTile closestTile = null;
        float closestDistance = 1e30f;

        ArrayList<HexagonTile> tiles = getTilesInRadius((int) axial.x, (int) axial.y, 4, true);

        Vector3f va = new Vector3f();
        Vector3f vb = new Vector3f();
        Vector3f camPos = mainCam.getGameObject().getTransform().getPosition();

        tiles.sort(
                (a, b) ->
                        Float.compare(
                                a.getGameObject()
                                        .getTransform()
                                        .getPosition(va)
                                        .distanceSquared(camPos),
                                b.getGameObject()
                                        .getTransform()
                                        .getPosition(vb)
                                        .distanceSquared(camPos)));

        for (HexagonTile tile : tiles) {
            float dist = cursorDistanceFromCenter(tile, mainCam, screenPos, pos);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestTile = tile;

                if (dist <= TransformHex.HEX_SIZE) return closestTile;
            }
        }

        return closestTile;
    }

    private float cursorDistanceFromCenter(
            HexagonTile tile, Camera cam, Vector2fc screenPos, Vector3f pos) {
        pos =
                cam.screenToPlane(
                        tile.getGameObject().getTransform(), screenPos.x(), screenPos.y(), pos);

        Vector2f axial = new Vector2f();

        // Convert those coordinates to axial
        TransformHex.cartesianToAxial(pos, axial);

        return (Math.abs(axial.x) + Math.abs(axial.y) + Math.abs(-axial.x - axial.y));
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onAwake() {
        Scene.getActiveScene().registerSingleton(this);
    }

    /** Spawns each HexagonTile as a GameObject */
    @Override
    public void onStart() {
        mTiles.getAllTiles()
                .forEach(
                        tile -> {
                            getGameObject().addChild(tile.getGameObject());
                        });
    }
}
