package org.dragonskulle.game.map;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IRenderUpdate;

public class HexagonMap extends Component implements IRenderUpdate {
    private HexagonTile[][][] map;

    @Override
    public void renderUpdate(double deltaTime) {

    }


    public void addTile(int q, int r, int s){
        this.map[q][r][s] = new HexagonTile(q,r,s);
    }

    public HexagonTile getTile(int q, int r, int s){
        try{
            return this.map[q][r][s];
        }catch (Exception e){ //rewrite without catch?
            return null;
        }
    }
}
