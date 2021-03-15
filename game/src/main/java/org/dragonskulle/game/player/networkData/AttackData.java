/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/** @author Oscar L */
public class AttackData implements INetSerializable {
    private HexagonTile mAttackingFrom;
    private HexagonTile mAttacking;

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mAttackingFrom.getTile().get().getQ());
        stream.writeInt(mAttackingFrom.getTile().get().getR());
        stream.writeInt(mAttackingFrom.getTile().get().getS());

        stream.writeInt(mAttacking.getTile().get().getQ());
        stream.writeInt(mAttacking.getTile().get().getR());
        stream.writeInt(mAttacking.getTile().get().getS());
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        this.mAttackingFrom = new HexagonTile(stream.readInt(), stream.readInt(), stream.readInt());
        this.mAttacking = new HexagonTile(stream.readInt(), stream.readInt(), stream.readInt());
    }

    public AttackData() {}

    public AttackData(Building attackingFrom, Building attacking) {
        this.mAttackingFrom = attackingFrom.getTile().get();
        this.mAttacking = attacking.getTile().get();
    }
}
