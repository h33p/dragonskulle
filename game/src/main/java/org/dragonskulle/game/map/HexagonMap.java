/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.GameConfig;
import org.dragonskulle.game.GameConfig.StatConfig;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.renderer.components.Camera;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * Store and synchronize the game map.
 *
 * @author Leela Muppala and Craig Wilbourne
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Accessors(prefix = "m")
@Log
public class HexagonMap extends NetworkableComponent implements IOnAwake {

    /**
     * This interface defines what should happen when you reach a node when using the flood fill
     * algorithm.
     *
     * @author DragonSkulle
     */
    public static interface IFloodFillVisitor {

        /**
         * The method which states what should happen when reaching a {@link HexagonTile}.
         *
         * @param map The {@link HexagonMap} to look through.
         * @param tile The {@link HexagonTile} to look through.
         * @param neighbours A {@link List} which will hold the neighbours of the tile.
         * @param tilesToFill This will hold the {@link HexagonTile}'s which need to be flooded
         */
        void onVisit(
                HexagonMap map,
                HexagonTile tile,
                List<HexagonTile> neighbours,
                Deque<HexagonTile> tilesToFill);
    }

    /** The size that is used to create the map. */
    @Getter @Setter private int mSize = 51;

    /** The map that is created which is made of a 2d array of HexagonTiles. */
    private HexagonTileStore mTiles;

    /** This will store what the largest landMass is. */
    private int[] mLargestLandMass;

    /** This will store what the next land mass number is. */
    private int mLandMass = 0;

    /** This will go through all the tiles and find all islands. */
    private void checkIslands() {

        mLargestLandMass = new int[2];
        mLargestLandMass[0] = -1;
        mLargestLandMass[1] = -1;
        getAllTiles()
                .forEach(
                        tile -> {
                            if (tile.mLandMassNumber == -1) {
                                floodFillLand(tile);
                            }
                        });
    }

    /**
     * This will use flood fill to find all connected tiles on land from the given {@link
     * HexagonTile}.
     *
     * @param tile The tile to start flooding from
     */
    private void floodFillLand(HexagonTile tile) {
        // Checks that we haven't already checked it
        int[] size = {0};
        if (tile.getTileType() != TileType.LAND || tile.mLandMassNumber != -1) {
            return;
        }

        Deque<HexagonTile> tiles = new ArrayDeque<HexagonTile>();
        tiles.add(tile);

        GameConfig cfg = GameState.getSceneConfig();
        int radius;
        if (cfg == null) {
            radius = 1;
        } else {
            StatConfig statCfg = cfg.getViewDistanceStat();
            radius = Math.round(statCfg.getValue().getBaseValue());
        }

        floodFill(
                tiles,
                (__, tileToUse, neighbours, tilesOut) -> {
                    if (tileToUse.getTileType() == TileType.LAND
                            && tileToUse.mLandMassNumber == -1) {
                        size[0]++;
                        tileToUse.mLandMassNumber = mLandMass;

                        for (HexagonTile neighbour : neighbours) {
                            if (neighbour.mLandMassNumber == -1
                                    && neighbour.getTileType() == TileType.LAND) {
                                tilesOut.add(neighbour);
                            }
                        }
                    }
                },
                radius);

        if (size[0] > mLargestLandMass[1]) {
            mLargestLandMass[0] = mLandMass;
            mLargestLandMass[1] = size[0];
        }

        mLandMass++;
    }

    /**
     * This will perform the flood fill algorithm.
     *
     * @param tiles The {@link Deque} to hold the tiles to which need to be visited.
     * @param visitor The {@link IFloodFillVisitor} which states what needs to be done when visiting
     *     a node.
     * @param radius The radius of the circle around the centre tile.
     */
    public void floodFill(Deque<HexagonTile> tiles, IFloodFillVisitor visitor, int radius) {
        ArrayList<HexagonTile> neighbours = new ArrayList<>();

        while (tiles.size() != 0) {
            HexagonTile tileToUse = tiles.removeFirst();
            getTilesInRadius(tileToUse, radius, false, neighbours);
            visitor.onVisit(this, tileToUse, neighbours, tiles);
        }
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
     * Get a stream of all hexagon tiles.
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
     * @param tilesOut A {@link List} where {@link HexagonTile}s in radius will be filled into.
     * @return A list of tiles within a radius of the selected tile, otherwise an empty ArrayList.
     */
    public List<HexagonTile> getTilesInRadius(
            HexagonTile tile, int radius, boolean includeTile, List<HexagonTile> tilesOut) {
        int minimum = includeTile ? 0 : 1;
        return getTilesInRadius(tile, minimum, radius, tilesOut);
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
     * @param tilesOut A {@link List} where {@link HexagonTile}s in radius will be filled into.
     * @return A {@link List} of {@link HexagonTile}s within the min and max radius. inclusive.
     */
    public List<HexagonTile> getTilesInRadius(
            HexagonTile tile, int min, int max, List<HexagonTile> tilesOut) {
        if (tile == null) return new ArrayList<HexagonTile>();

        return getTilesInRadius(tile.getQ(), tile.getR(), min, max, tilesOut);
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
     * @param tilesOut A {@link List} where {@link HexagonTile}s in radius will be filled into.
     * @return An {@link List} of {@link HexagonTile}s within the min and max radius, inclusive.
     */
    private List<HexagonTile> getTilesInRadius(
            int tileQ, int tileR, int min, int max, List<HexagonTile> tilesOut) {
        tilesOut.clear();
        for (int q = -max; q <= max; q++) {
            // Only generate valid tile coordinates.
            int lower = Math.max(-max, -q - max);
            int upper = Math.min(max, -q + max);
            for (int r = lower; r <= upper; r++) {
                int s = -q - r;

                // Ensure tile isn't within the minimum.
                int distance = getDistance(q, r, s);
                if (distance < min) {
                    continue;
                }

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = getTile(tileQ + q, tileR + r);
                if (selectedTile == null) {
                    continue;
                }

                // Add the tile to the list.
                tilesOut.add(selectedTile);
            }
        }

        return tilesOut;
    }

    /**
     * Calculate the distance from the centre (0, 0, 0).
     *
     * @param q the q coordinate
     * @param r the r coordinate
     * @param s the s coordinate
     * @return distance
     */
    private int getDistance(int q, int r, int s) {
        return Math.max(Math.max(Math.abs(q), Math.abs(r)), Math.abs(s));
    }

    /**
     * Convert cursor position to the tile it is over.
     *
     * @return hexagon tile the mouse cursor is over.
     */
    public HexagonTile cursorToTile() {
        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);

        if (mainCam == null) {
            return null;
        }

        // Retrieve scaled screen coordinates
        Cursor cursor = Actions.getCursor();

        if (cursor == null) {
            return null;
        }

        Vector2fc screenPos = cursor.getPosition();

        // Convert those coordinates to local coordinates within the map
        Vector3f pos =
                mainCam.screenToPlane(
                        getGameObject().getTransform(),
                        0,
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

        List<HexagonTile> tiles =
                getTilesInRadius((int) axial.x, (int) axial.y, 0, 4, new ArrayList<>());

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

                if (dist <= TransformHex.HEX_SIZE) {
                    return closestTile;
                }
            }
        }

        return closestTile;
    }

    /**
     * How far away the cursor is from the center of a tile.
     *
     * @param tile tile to check
     * @param cam camera to check from.
     * @param screenPos cursor position on screen.
     * @param pos output position of the cursor on the tile plane.
     * @return distance from cursor to tile center.
     */
    private float cursorDistanceFromCenter(
            HexagonTile tile, Camera cam, Vector2fc screenPos, Vector3f pos) {
        pos =
                cam.screenToPlane(
                        tile.getGameObject().getTransform(), 0, screenPos.x(), screenPos.y(), pos);

        Vector2f axial = new Vector2f();

        // Convert those coordinates to axial
        TransformHex.cartesianToAxial(pos, axial);

        return (Math.abs(axial.x) + Math.abs(axial.y) + Math.abs(-axial.x - axial.y));
    }

    /**
     * Calculate a rectangle bounding box around all visible (non-fog) tiles.
     *
     * <p>If there are no tiles, this method will set min to POSITIVE_INFINITY, and max to
     * NEGATIVE_INFINITY.
     *
     * @param min target minimum coordinates
     * @param max target maximum coordinates
     */
    public void calculateVisibleTileBounds(Vector3f min, Vector3f max) {
        min.set(Float.POSITIVE_INFINITY);
        max.set(Float.NEGATIVE_INFINITY);

        Vector3f mTmpPos = new Vector3f();

        getAllTiles()
                .filter(t -> t.getTileType() != TileType.FOG)
                .map(t -> t.getGameObject().getTransform().getPosition(mTmpPos))
                .forEach(
                        p -> {
                            min.min(p);
                            max.max(p);
                        });

        min.sub(2, 2, 0);
        max.add(2, 2, 0);
    }

    /**
     * This checks if the given {@link HexagonTile} is an island (An island is defined as a land
     * mass which is disconnected completely from the largest land mass).
     *
     * @param tile The {@link HexagonTile} to check if its in an island
     * @return Returns {@code true} if it is an island, {@code false} if not
     */
    public boolean isIsland(HexagonTile tile) {
        if (mLargestLandMass == null) {
            log.severe("ERROR");
        }

        return tile.mLandMassNumber != mLargestLandMass[0];
    }

    @Override
    protected void onNetworkInitialise() {

        GameState gameState = Scene.getActiveScene().getSingleton(GameState.class);

        if (gameState != null) {
            mSize = gameState.getConfig().getGlobal().getMapSize();
        }

        Random rand = new Random();
        mTiles = new HexagonTileStore(mSize, rand.nextInt(), this);
        checkIslands();
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onAwake() {

        if (mSize <= 0) {
            log.severe("Map size must be greater than 0!");
            getGameObject().destroy();
            return;
        }

        Scene.getActiveScene().registerSingleton(this);
    }

    /**
     * Update the visual game object of the tile.
     *
     * @param tile hexagon tile to update.
     */
    void updateTileGameObject(HexagonTile tile) {
        getGameObject().addChild(tile.getGameObject());
    }
}
