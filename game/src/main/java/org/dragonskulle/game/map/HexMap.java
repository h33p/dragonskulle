/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

/**
 * @author Leela Muppala
 *     <p>This class generates and stores a map The hexagon map object
 */
class HexagonMap extends Component implements IOnStart {

    @Getter
    private  int size;

    @Getter
    private HexagonTile[][] map;

    @Getter
    private GameObject[][] mapObject;

    /** @param size - the size of the map */
    HexagonMap(int size) {
        this.size = size;
        if (size < 0) {
            throw new IllegalArgumentException("The size must greater than 0");
        }

        this.map = createHexMap();

    }

    /**
     * @param input - The size of the hexagon map
     * @return - an int which gives the number of nulls to add in the 2d array to generate a hexagon shape
     * Can only input odd numbered size due to hexagon shape
     */
    private int nulls (int input) {
         if (input%2 == 0 ) {
             throw new IllegalArgumentException("the input size must be an odd number");
         }
         return (input -1) / 2;
    }

    /** Hex(q,r) is stored as array[r][q]
     * Map is automatically created and stored in HexMap
     * @return - Returns the HexMap*/

    HexagonTile[][] createHexMap() {
        int max_empty = nulls(size); // The max number of empty spaces in one row of the array
        int empty = max_empty;
        HexagonTile[][] map = new HexagonTile[size][size];
        int loop = size / 2;

        /** Generates the first part of the map */
        for (int r = 0; r < loop; r++) {
            int inside_empty = empty;
            for (int q = 0; q < size; q++) {
                if (inside_empty > 0) {
                    map[r][q] = null; // No tile in this location
                    inside_empty--;
                } else if (inside_empty == 0) {

//                    GameObject hexagon = new GameObject("hexagon");
//                    HexagonTile tile = new HexagonTile(q, r, -q-r);
//                    hexagon.addComponent(tile);

                    HexagonTile tile = new HexagonTile(q, r, -q-r);
                    map[r][q] = tile;
                }
            }
            empty--;
        }

        /** Generates the middle part of the map */
        int r_m = (size / 2);
        for (int q = 0; q < size; q++) {
            HexagonTile tile = new HexagonTile(q, r_m, -q - r_m);
            map[r_m][q] = tile;
        }

        /** Generates the last part of the map */
        loop = (size / 2) + 1;
        int min_val = size - max_empty;
        int current_val = size;
        for (int r = loop; r < size; r++) {
            current_val--; // The number of cells with actual coordinates, it decreases with every
            // row
            int inside_val = 1;
            for (int q = 0; q < size; q++) {
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

        mapObject = new GameObject[size][size];
        GameObject hexagon = new GameObject("hexagon");
        //hexagon.addComponent(new Renderable());
        for (int r = 0; r < size; r++) {
            for (int q = 0; q < size; q++) {

                if (map[r][q] != null) {
                    //TODO set correct transform
                    GameObject.instantiate(hexagon);

                }
            }
        }

    }
}
