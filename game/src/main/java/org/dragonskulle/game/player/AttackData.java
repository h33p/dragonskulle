package org.dragonskulle.game.player;

import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.requests.INeedToTalkToTheServer;
import org.dragonskulle.network.components.sync.INetSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Oscar L
 */
public class AttackData implements INetSerializable {
    private HexagonTile mAttackingFrom;
    private HexagonTile mAttacking;

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mAttackingFrom.getQ());
        stream.writeInt(mAttackingFrom.getR());
        stream.writeInt(mAttackingFrom.getS());

        stream.writeInt(mAttacking.getQ());
        stream.writeInt(mAttacking.getR());
        stream.writeInt(mAttacking.getS());
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        this.mAttackingFrom = new HexagonTile(stream.readInt(), stream.readInt(), stream.readInt());
        this.mAttacking = new HexagonTile(stream.readInt(), stream.readInt(), stream.readInt());
    }

    public AttackData() {
    }

    public AttackData(Building attackingFrom, Building attacking) {
        this.mAttackingFrom = attackingFrom.getTile().get();
        this.mAttacking = attacking.getTile().get();
    }
}
