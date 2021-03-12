/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

/**
 * @author Leela Muppala
 *     <p>The hexagonMap GameObject is initialised here.
 */
@Accessors(prefix = "m")
public class MapTest {
    public static void main(String[] args) {

        /** Created the hexagonMap GameObject */
        GameObject hexagonMap = new GameObject("hexagonMap");
        hexagonMap.addComponent(new HexagonMap(11));
        hexagonMap.addComponent(new Renderable());

        /** Adds a reference to the hexagonMap */
        Reference<HexagonMap> myComponent = hexagonMap.getComponent(HexagonMap.class);
        HexagonMap myObject = myComponent.get();
        HexagonTile[][] map = myObject.getTiles();

        /** Visually prints out the map */
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 11; j++) {
                String str = map[i][j] == null ? "null " : map[i][j].toString();
                System.out.print(str);
            }
            System.out.println();
        }
    }
}
