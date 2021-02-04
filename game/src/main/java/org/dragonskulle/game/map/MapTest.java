package org.dragonskulle.game.map;

public class MapTest {
    public static void main(String[] args){
        HexagonTile tile = new HexagonTile(0,0,0);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());

        tile = new HexagonTile(100,-100,0);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());

        tile = new HexagonTile(100,0,-100);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());

        tile = new HexagonTile(0,100,-100);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());

        tile = new HexagonTile(-100,100,0);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());

        tile = new HexagonTile(0,-100,100);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());

        tile = new HexagonTile(-100,0,100);
        System.out.println("tile: " + tile.toString());
        System.out.println("cartesian: " + tile.toCartesian());
//        HexagonMap map = new HexagonMap();
//        map.addTile(2,3,4);
//        System.out.println("added");
//        System.out.println(map.getTile(2,3,4).toString());
    }
}
