/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.core.GameObject;

/**
 * @author Leela Muppala
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Accessors(prefix = "m")
@Log
class HexagonMap extends Component implements IOnStart {

    /** The size that is used to create the map. */
    @Getter private final int mSize;

    /** The map that is created which is made of a 2d array of HexagonTiles. */
    @Getter private HexagonTile[][] mTiles;

    /** A similar map to that of mMap made of a 2d array of HexagonTile gameObjects. */
    @Getter private GameObject[][] mGameObjectMap;

    /**
     * HexagonMap constructor that gets the size for the map and calls the createHexMap function to
     * create the map.
     *
     * @param size the size of the map
     */
    HexagonMap(int size) {
        this.mSize = size;

        if (size < 0) {
            size = 0;
            log.warning("The size should be greater than 0");
        }

        this.mTiles = createTiles();
    }

    /**
     * Provides a number that shows the number of nulls to add to a HexagonMap. Can only input odd
     * numbered size due to hexagon shape.
     *
     * @param input - The size of the hexagon map.
     * @return Returns the number of spaces to add in the 2d array to generate a hexagon shape.
     */
    private int getSpaces(int input) {
        if (input % 2 == 0) {
            log.warning("The size is not an odd number");
        }
        return (input - 1) / 2;
    }

    /**
     * Hex(q,r) is stored as array[r][q] Map is created and stored in HexMap.
     *
     * @return Returns a 2d array of HexagonTiles.
     */
    private HexagonTile[][] createTiles() {
        int max_empty = getSpaces(mSize); // The max number of empty spaces in one row of the array
        HexagonTile[][] map = new HexagonTile[mSize][mSize];
        int loop = mSize / 2;
        int empty = max_empty;

        /** Generates the first part of the map */
        for (int r = 0; r < loop; r++) {

            int inside_empty = empty;

            for (int q = 0; q < mSize; q++) {
                if (inside_empty > 0) {

                    map[r][q] = null; // No tile in this location
                    inside_empty--;

                } else if (inside_empty == 0) {

                    HexagonTile tile = new HexagonTile(q, r, -q - r);
                    map[r][q] = tile;
                }
            }
            empty--;
        }

        /** Generates the middle part of the map */
        int r_m = (mSize / 2);
        for (int q = 0; q < mSize; q++) {

            HexagonTile tile = new HexagonTile(q, r_m, -q - r_m);
            map[r_m][q] = tile;
        }

        /** Generates the last part of the map */
        loop = (mSize / 2) + 1;
        int min_val = mSize - max_empty;
        int current_val = mSize;
        for (int r = loop; r < mSize; r++) {
            current_val--; // The number of cells with actual coordinates, it decreases with every
            // row
            int inside_val = 1;
            for (int q = 0; q < mSize; q++) {

                if (inside_val <= current_val) {

                    HexagonTile tile = new HexagonTile(q, r, -q - r);
                    map[r][q] = tile;

                    inside_val++;
                } else {

                    map[r][q] = null;
                }
            }
        }
        return map;
    }

    @Override
    public void onDestroy() {}

    /** Spawns each HexagonTile as a GameObject */
    @Override
    public void onStart() {

        mGameObjectMap = new GameObject[mSize][mSize];
        GameObject hexagon = new GameObject("hexagon");
        hexagon.addComponent(new Renderable());

        for (int r = 0; r < mSize; r++) {

            for (int q = 0; q < mSize; q++) {

                if (mTiles[r][q] != null) {
                    // TODO set correct transform
                    mGameObjectMap[r][q] = GameObject.instantiate(hexagon);
                }
            }
        }
    }
}
