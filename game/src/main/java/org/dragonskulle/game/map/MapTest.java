/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;
import java.lang.System.*;

public class MapTest {
    public static void main(String[] args) {
        //HexagonTile tile = new HexagonTile(1,-1,0);

        HexMap grid = new HexMap(5);
        HexagonTile[][] map = grid.createHexMap();

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                String str =  map[i][j] == null ? "null " :map[i][j].toString();
                System.out.print(str);
            }
            System.out.println();
        }

    }
}
