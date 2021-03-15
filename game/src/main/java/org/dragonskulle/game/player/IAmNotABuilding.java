package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.game.map.HexagonTile;

import java.sql.Statement;
import java.util.List;

/**
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class IAmNotABuilding {
    @Getter
    private int mToken = 0;

    @Getter
    private Stat mStats;
    private List<HexagonTile> hexTiles;

    public List<IAmNotABuilding> attackableBuildings() {
        return null;
    }

    public List<HexagonTile> getHexTiles() {
        return hexTiles;
    }

    public void setHexTiles(List<HexagonTile> hexTiles) {
        this.hexTiles = hexTiles;
    }

    public int getR() {
        return 0;
    }

    public int getS() {
        return 0;
    }
}
