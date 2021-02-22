/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;
import java.lang.System.*;

public class MapTest {
    public static void main(String[] args) {
        HexagonTile tile = new HexagonTile(1,-1,0);

        HexMap grid = new HexMap(9);
        HexagonTile[][] map = grid.createHexMap();

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                String str =  map[i][j] == null ? "null " :map[i][j].toString();
                System.out.print(str);
            }
            System.out.println();
        }


//        // Creates Map
//        HexagonMap map = new HexagonMap();
//        HexagonTile tile;
//        // Creates and adds tiles to map
//        try {
//            map.addTile(0, 0, 0);
//            map.addTile(100, -100, 0);
//            try {
//                map.addTile(100, -100, 0); // should throw error as already defined
//            } catch (Exception e) {
//                System.out.println("Failed to create tile as already exists");
//            }
//            tile = map.getTile(0, 0, 0);
//            System.out.println("tile: " + tile.toString());
//            System.out.println("cartesian: " + tile.getCartesian());
//
//            tile = map.getTile(100, -100, 0);
//            System.out.println("tile: " + tile.toString());
//            System.out.println("cartesian: " + tile.getCartesian());
//
//            tile = map.getTile(100, -66, 0); // tile doesnt exist
//            System.out.println("tile: " + tile.toString());
//            System.out.println("cartesian: " + tile.getCartesian());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
