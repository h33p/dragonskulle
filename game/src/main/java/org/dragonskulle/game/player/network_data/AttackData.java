/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.network_data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which will contain all the data to be sent for attacking.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class AttackData implements INetSerializable {
    private int mAttackerQ;
    private int mAttackerR;
    private int mDefenderQ;
    private int mDefenderR;

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
        stream.writeInt(mAttackerQ);
        stream.writeInt(mAttackerR);

        stream.writeInt(mDefenderQ);
        stream.writeInt(mDefenderR);
    }

    @Override
    public void deserialize(DataInput stream) throws IOException {
        mAttackerQ = stream.readInt();
        mAttackerR = stream.readInt();
        mDefenderQ = stream.readInt();
        mDefenderR = stream.readInt();
    }

    /** Constructor. */
    public AttackData() {}

    /**
     * Constructor.
     *
     * @param attackingFrom The attacker building
     * @param attacking The defending building
     */
    public AttackData(Building attackingFrom, Building attacking) {
        setData(attackingFrom, attacking);
    }

    /**
     * Sets the request data.
     *
     * @param attackingFrom The attacker building
     * @param attacking The defending building
     */
    public void setData(Building attackingFrom, Building attacking) {
        HexagonTile fromTile = attackingFrom.getTile();
        mAttackerQ = fromTile.getQ();
        mAttackerR = fromTile.getR();
        HexagonTile toTile = attacking.getTile();
        mDefenderQ = toTile.getQ();
        mDefenderR = toTile.getR();
    }

    /**
     * Gets the attacking building from the map.
     *
     * @param map the map
     * @return the attacking building
     */
    public Building getAttacker(HexagonMap map) {
        HexagonTile tile = map.getTile(mAttackerQ, mAttackerR);
        return tile == null ? null : tile.getBuilding();
    }

    /**
     * Gets the defending building from the map.
     *
     * @param map the map
     * @return the defending building
     */
    public Building getDefender(HexagonMap map) {
        HexagonTile tile = map.getTile(mDefenderQ, mDefenderR);
        return tile == null ? null : tile.getBuilding();
    }
}
