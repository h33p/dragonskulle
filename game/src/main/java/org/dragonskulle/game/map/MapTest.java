/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import org.dragonskulle.components.Renderable;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

import java.lang.System.*;

public class MapTest {
    public static void main(String[] args) {
        // HexagonTile tile = new HexagonTile(1,-1,0);

        GameObject hexagonMap = new GameObject("hexagonMap");
        hexagonMap.addComponent(new HexagonMap(11));
        //hexagonMap.addComponent(new Renderable());

        Reference<HexagonMap> myComponent = hexagonMap.getComponent(HexagonMap.class);
        HexagonMap myObject = myComponent.get();
        HexagonTile[][] map = myObject.getMap();

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 11; j++) {
                String str = map[i][j] == null ? "null " : map[i][j].toString();
                System.out.print(str);
            }
            System.out.println();
        }
    }
}
