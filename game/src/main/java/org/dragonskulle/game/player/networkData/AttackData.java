/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which will contain all the data to be sent for attacking
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class AttackData implements INetSerializable {
    private int mFromQ;
    private int mFromR;
    private int mToQ;
    private int mToR;

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mFromQ);
        stream.writeInt(mFromR);

        stream.writeInt(mToQ);
        stream.writeInt(mToR);
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        mFromQ = stream.readInt();
        mFromR = stream.readInt();
        mToQ = stream.readInt();
        mToR = stream.readInt();
    }

    public AttackData() {}

    /**
     * Constructor
     *
     * @param attackingFrom The attacker building
     * @param attacking The defending building
     */
    public AttackData(Building attackingFrom, Building attacking) {
        HexagonTile fromTile = attackingFrom.getTile();
        mFromQ = fromTile.getQ();
        mFromR = fromTile.getR();
        HexagonTile toTile = attacking.getTile();
        mToQ = toTile.getQ();
        mToR = toTile.getR();
    }

    public Building getAttacker(HexagonMap map) {
        HexagonTile tile = map.getTile(mFromQ, mFromR);
        return tile == null ? null : tile.getBuilding();
    }

    public Building getDefender(HexagonMap map) {
        HexagonTile tile = map.getTile(mToQ, mToR);
        return tile == null ? null : tile.getBuilding();
    }
}
