/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which will contain all the data to be sent for attacking
 * @author DragonSkulle
 *
 */
@Accessors(prefix = "m")
public class AttackData implements INetSerializable {
    @Getter private HexagonTile mAttackingFrom;
    @Getter private HexagonTile mAttacking;

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

    public AttackData() {}

    /**
     * Constructor
     * @param attackingFrom The attacker building 
     * @param attacking The defending building
     */
    public AttackData(Building attackingFrom, Building attacking) {
        this.mAttackingFrom = attackingFrom.getTile().get();
        this.mAttacking = attacking.getTile().get();
    }
}
