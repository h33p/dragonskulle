/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.network_data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which holds the data so buildings can be sold.
 *
 * @author DragonSkulle
 */
public final class SellData implements INetSerializable {

    private int mQ;
    private int mR;

    /** Constructor. */
    public SellData() {}

    /**
     * The Constructor.
     *
     * @param toSell The building to sell
     */
    public SellData(Building toSell) {
        setData(toSell);
    }

    /**
     * Sets the request's data.
     *
     * @param toSell The building to sell
     */
    public void setData(Building toSell) {
        HexagonTile tileToSell = toSell.getTile();

        mQ = tileToSell.getQ();
        mR = tileToSell.getR();
    }

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
        stream.writeInt(mQ);
        stream.writeInt(mR);
    }

    @Override
    public void deserialize(DataInput stream) throws IOException {
        mQ = stream.readInt();
        mR = stream.readInt();
    }

    /**
     * Gets the building which the data is referencing.
     *
     * @param map the map
     * @return the building to sell
     */
    public Building getBuilding(HexagonMap map) {
        HexagonTile tile = map.getTile(mQ, mR);
        return tile == null ? null : tile.getBuilding();
    }
}
