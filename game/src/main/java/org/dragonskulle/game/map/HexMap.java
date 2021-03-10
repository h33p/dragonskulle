/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.core.GameObject;

/**
 * @author Leela Muppala
 *     <p>This class generates and stores a map The hexagon map object
 */
class HexagonMap extends Component implements IOnStart {

    @Getter private int mSize;

    @Getter private HexagonTile[][] mMap;

    @Getter private GameObject[][] mMapObject;

    /** @param size - the size of the map */
    HexagonMap(int size) {
        this.mSize = size;
        if (size < 0) {
            throw new IllegalArgumentException("The size must greater than 0");
        }

        this.mMap = createHexMap();
    }

    /**
     * @param input - The size of the hexagon map
     * @return - an int which gives the number of nulls to add in the 2d array to generate a hexagon
     *     shape Can only input odd numbered size due to hexagon shape
     */
    private int nulls(int input) {
        if (input % 2 == 0) {
            throw new IllegalArgumentException("the input size must be an odd number");
        }
        return (input - 1) / 2;
    }

    /**
     * Hex(q,r) is stored as array[r][q] Map is automatically created and stored in HexMap
     *
     * @return - Returns the HexMap
     */
    HexagonTile[][] createHexMap() {
        int max_empty = nulls(mSize); // The max number of empty spaces in one row of the array
        int empty = max_empty;
        HexagonTile[][] map = new HexagonTile[mSize][mSize];
        int loop = mSize / 2;

        /** Generates the first part of the map */
        for (int r = 0; r < loop; r++) {
            int inside_empty = empty;
            for (int q = 0; q < mSize; q++) {
                if (inside_empty > 0) {
                    map[r][q] = null; // No tile in this location
                    inside_empty--;
                } else if (inside_empty == 0) {

                    //                    GameObject hexagon = new GameObject("hexagon");
                    //                    HexagonTile tile = new HexagonTile(q, r, -q-r);
                    //                    hexagon.addComponent(tile);

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

    @Override
    public void onStart() {

        mMapObject = new GameObject[mSize][mSize];
        GameObject hexagon = new GameObject("hexagon");
        hexagon.addComponent(new Renderable());
        for (int r = 0; r < mSize; r++) {
            for (int q = 0; q < mSize; q++) {

                if (mMap[r][q] != null) {
                    // TODO set correct transform
                    GameObject.instantiate(hexagon);
                }
            }
        }
    }
}
