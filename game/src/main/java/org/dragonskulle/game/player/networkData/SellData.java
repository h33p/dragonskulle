/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.requests.INeedToTalkToTheServer;
import org.dragonskulle.network.components.sync.INetSerializable;

/** @author Oscar L */
public final class SellData implements INetSerializable {

    private HexagonTile mTile;

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mTile.getQ());
        stream.writeInt(mTile.getR());
        stream.writeInt(mTile.getS());
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        this.mTile = new HexagonTile(stream.readInt(), stream.readInt(), stream.readInt());
    }

    public SellData() {}

    public SellData(Building toSell) {
        mTile = toSell.getTile().get();
    }
}
