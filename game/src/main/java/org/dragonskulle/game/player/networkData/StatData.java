/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which holds the data to be sent for upgrading stats
 *
 * @author low101043
 */
public class StatData implements INetSerializable {

    private Building mBuilding;
    private SyncStat<?> mStat;

    public StatData() {}

    /**
     * The Constructor
     *
     * @param building The building to upgrade
     * @param stat The stat to upgrade
     */
    public StatData(Building building, SyncStat<?> stat) {
        mBuilding = building;
        mStat = stat;
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mBuilding.getTile().getQ());
        stream.writeInt(mBuilding.getTile().getR());
        stream.writeInt(mBuilding.getTile().getS());
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        // TODO Auto-generated method stub

    }
}
