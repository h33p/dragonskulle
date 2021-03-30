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
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;

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
            size = 0;
            log.warning("The size should be greater than 0");
        }

        mTiles = new HexagonTileStore(mSize);
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
     * @deprecated Use {@link HexagonTile}{@code .getBuilding()} instead.
     *     <p>Get the building at the specified position, or {@code null} if the building does not
     *     exist.
     * @param q The q coordinate.
     * @param r The r coordinate.
     * @return The building, or {@code null} if there is no building at that position.
     */
    public Building getBuilding(int q, int r) {
        HexagonTile tile = getTile(q, r);

        if (tile == null) return null;

        return tile.getBuilding();
    }

    /**
     * @deprecated This is now done within each {@link Building}.
     *     <p>Store a reference to the {@link Building} at the specified position.
     * @param building The Building to be stored.
     * @param q The q coordinate.
     * @param r The r coordinate.
     * @return {@code true} if the building was stored. {@code false} if coordinate was invalid, or
     *     building already exists at the location (remove it first).
     */
    public boolean storeBuilding(Building building, int q, int r) {
        HexagonTile tile = getTile(q, r);

        if (tile == null || tile.getBuilding() != null) return false;

        tile.setBuilding(building);

        return true;
    }

    /**
     * @deprecated This should be done directly via {@link Building#remove()}.
     *     <p>Stop storing the reference to the Building at the specified position.
     *     <p>Stores {@code null} at the position instead.
     * @param q The q coordinate.
     * @param r The r coordinate.
     * @return {@code true} if tile was valid, {@code false} otherwise.
     */
    public boolean removeBuilding(int q, int r) {
        HexagonTile tile = getTile(q, r);

        if (tile == null) return false;

        tile.setBuilding(null);

        return true;
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
        ArrayList<HexagonTile> tiles = getTilesInRadius(tile, radius);
        if (tile != null && includeTile) {
            tiles.add(tile);
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
        ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();
        if (tile == null) return tiles;

        // Get the tile's q and r coordinates.
        int qCentre = tile.getQ();
        int rCentre = tile.getR();

        for (int rOffset = -radius; rOffset <= radius; rOffset++) {
            for (int qOffset = -radius; qOffset <= radius; qOffset++) {
                // Only get tiles whose s coordinates are within the desired range.
                int sOffset = -qOffset - rOffset;

                // Do not include tiles outside of the radius.
                if (sOffset > radius || sOffset < -radius) continue;
                // Do not include the building's HexagonTile.
                if (qOffset == 0 && rOffset == 0) continue;

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = getTile(qCentre + qOffset, rCentre + rOffset);
                if (selectedTile == null) continue;

                // Add the tile to the list.
                tiles.add(selectedTile);
            }
        }

        return tiles;
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
