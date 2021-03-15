/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;

/**
 * @author Leela Muppala
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Accessors(prefix = "m")
@Log
public class HexagonMap extends Component implements IOnStart {

    /** The size that is used to create the map. */
    @Getter private final int mSize;

    /** The map that is created which is made of a 2d array of HexagonTiles. */
    @Getter private HexagonTileStore mTiles;

    /** A similar map to that of mMap made of a 2d array of HexagonTile gameObjects. */
    @Getter private GameObject[][] mGameObjectMap;

    /**
     * HexagonMap constructor that gets the size for the map and calls the createHexMap function to
     * create the map.
     *
     * @param size the size of the map
     */
    public HexagonMap(int size) {
        this.mSize = size;

        if (size < 0) {
            size = 0;
            log.warning("The size should be greater than 0");
        }

        mTiles = new HexagonTileStore(mSize);
    }

    public HexagonTile getTile(int q, int r) {
        return mTiles.getTile(q, r);
    }

    @Override
    public void onDestroy() {}

    /** Spawns each HexagonTile as a GameObject */
    @Override
    public void onStart() {
        mGameObjectMap = new GameObject[mSize][mSize];

        mTiles.getAllTiles()
                .forEach(
                        tile -> {
                            getGameObject().addChild(tile.getGameObject());
                        });
    }
}
