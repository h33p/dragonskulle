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

        if (size <= 0) {
            throw new RuntimeException("The size must be greater than 0");
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
            HexagonTile tile, int distance, boolean includeTile) {
        int minimum = includeTile ? 0 : 1;
        return getTilesInRadius(tile, minimum, distance);
    }

    /**
     * Get the tiles within a minimum and maximum radius of the target tile.
     *
     * <p>A min radius of 0 will include the target tile. <br>
     * A min radius of 1 will exclude the target tile.
     *
     * @param tile The target tile.
     * @param min The minimum radius of tiles to include.
     * @param max The maximum radius of tiles to include.
     * @return An {@link ArrayList} of {@link HexagonTile}s within the min and max radius,
     *     inclusive.
     */
    public ArrayList<HexagonTile> getTilesInRadius(HexagonTile tile, int min, int max) {
        if (tile == null) return new ArrayList<HexagonTile>();

        return getTilesInRadius(tile.getQ(), tile.getR(), min, max);
    }

    /**
     * Get the tiles within a minimum and maximum radius of the target tile position.
     *
     * <p>A min radius of 0 will include the target tile. <br>
     * A min radius of 1 will exclude the target tile.
     *
     * <p>Based off pseudocode found here: https://www.redblobgames.com/grids/hexagons/#range
     *
     * @param tileQ The target tile Q position.
     * @param tileR The target tile R position.
     * @param min The minimum radius of tiles to include.
     * @param max The maximum radius of tiles to include.
     * @return An {@link ArrayList} of {@link HexagonTile}s within the min and max radius,
     *     inclusive.
     */
    private ArrayList<HexagonTile> getTilesInRadius(int tileQ, int tileR, int min, int max) {
        ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();

        for (int q = -max; q <= max; q++) {
            // Only generate valid tile coordinates.
            int lower = Math.max(-max, -q - max);
            int upper = Math.min(max, -q + max);
            for (int r = lower; r <= upper; r++) {
                int s = -q - r;

                // Ensure tile isn't within the minimum.
                int distance = getDistance(q, r, s);
                if (distance < min) continue;

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = getTile(tileQ + q, tileR + r);
                if (selectedTile == null) continue;

                // Add the tile to the list.
                tiles.add(selectedTile);
            }
        }

        return tiles;
    }

    /**
     * Calculate the distance from the centre (0, 0, 0).
     *
     * @return
     */
    private int getDistance(int q, int r, int s) {
        return Math.max(Math.max(Math.abs(q), Math.abs(r)), Math.abs(s));
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

        ArrayList<HexagonTile> tiles = getTilesInRadius((int) axial.x, (int) axial.y, 0, 4);

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
