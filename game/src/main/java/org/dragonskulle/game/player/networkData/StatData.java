/* (C) 2021 DragonSkulle */

package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.experimental.Accessors;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which holds the data to be sent for upgrading stats.
 *
 * @author low101043
 */
@Accessors(prefix = "m")
public class StatData implements INetSerializable {

    public SyncStat<?> mStat;

    private int mQ;
    private int mR;

    public StatData() {}

    public StatData(Building building, SyncStat<?> stat) {
        setData(building, stat);
    }

    public void setData(Building building, SyncStat<?> stat) {
        mQ = building.getTile().getQ();
        mR = building.getTile().getR();
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

    public HexagonTile getTile(HexagonMap map) {
        return map.getTile(mQ, mR);
    }

    public Building getBuilding(HexagonMap map) {
        if (getTile(map) == null) {
            return null;
        }
        return getTile(map).getBuilding();
    }
}
