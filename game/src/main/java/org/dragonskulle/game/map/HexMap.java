package org.dragonskulle.game.map;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IRenderUpdate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Leela Muppala
 * <p> This class generates and stores a map</p>
 */

/** The hexagon map object*/
public class HexMap extends Component {

    public final int size;

    /**
     * @param size - the size of the map
     */
    public HexMap(int size) {
        this.size = size;
        if (size < 0 ) {
           throw new IllegalArgumentException("The size must greater than 0");
        }
    }

    /** HashMap to find the the max number of nulls to add in the 2d array when storing a map
     * Currently the sizes are restricted to 1 - 27
     * */
    private static final Map<Integer,Integer> nulls = new HashMap<Integer, Integer>(){{
        put(1,0);
        put(3,1);
        put(5,2);
        put(7,3);
        put(9,4);
        put(11,5);
        put(13,6);
        put(15,7);
        put(17,9);
        put(19,10);
        put(21,11);
        put(23,13);
        put(25,15);
        put(27,17);
    }};

    /** Hex(q,r) is stored as array[r][q] */

    public HexagonTile[][] createHexMap() {

        int max_empty = nulls.get(size); // The max number of empty spaces in one row of the array
        int empty = max_empty;
        HexagonTile[][] map = new HexagonTile[size][size];
        int loop = size/2;

        /** Generates the first part of the map*/
        for (int r = 0; r < loop; r++) {
            int inside_empty = empty;
            for (int q = 0; q < size; q++) {
                if (inside_empty > 0) {
                    map[r][q] = null; // No tile in this location
                    inside_empty--;
                }
                else if (inside_empty == 0){
                    HexagonTile tile = new HexagonTile(q, r, -q-r);
                    map[r][q] = tile;
                }
            }
            empty--;
        }

        /** Generates the middle part of the map*/
        int r_m = (size/2);
        for (int q = 0; q < size; q++) {
            HexagonTile tile = new HexagonTile(q, r_m, -q-r_m);
            map[r_m][q] = tile;
        }

        /**Generates the last part of the map*/
        loop = (size/2) + 1;
        int min_val = size-max_empty;
        int current_val = size;
        for (int r = loop; r < size; r++) {
            current_val--; //The number of cells with actual coordinates, it decreases with every row
            int inside_val = 1;
            for (int q = 0; q < size; q++) {
                if (inside_val <= current_val) {
                    HexagonTile tile = new HexagonTile(q, r, -q-r);
                    map[r][q] = tile;
                    inside_val++;
                }
                else {
                    map[r][q] = null;
                }
            }
        }

        return map;
    }

}
