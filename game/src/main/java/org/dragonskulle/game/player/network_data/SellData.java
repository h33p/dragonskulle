/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.network_data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which holds the data so buildings can be sold
 *
 * @author DragonSkulle
 */
public final class SellData implements INetSerializable {

    private int mQ;
    private int mR;

    public SellData() {}

    /**
     * The Constructor
     *
     * @param toSell The building to sell
     */
    public SellData(Building toSell) {
        setData(toSell);
    }

    /**
     * Sets the request's data
     *
     * @param toSell The building to sell
     */
    public void setData(Building toSell) {
        HexagonTile tileToSell = toSell.getTile();

        mQ = tileToSell.getQ();
        mR = tileToSell.getR();
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mQ);
        stream.writeInt(mR);
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        mQ = stream.readInt();
        mR = stream.readInt();
    }

    public Building getBuilding(HexagonMap map) {
        HexagonTile tile = map.getTile(mQ, mR);
        return tile == null ? null : tile.getBuilding();
    }
}
