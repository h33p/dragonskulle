/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.network_data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.experimental.Accessors;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.Stat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * Holds data to allow a {@link Building} to increase a specific {@link Stat}.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
public class StatData implements INetSerializable {

    /** The ID of the {@link Stat} to change. */
    private int mStatID;
    /** The q coordinate of the desired {@link Building}. */
    private int mQ;
    /** The r coordinate of the desired {@link Building}. */
    private int mR;

    public StatData() {}

    /**
     * Store data for increasing a specific {@link Stat} for a {@link Building}.
     *
     * @param building The Building whose Stat will increase.
     * @param stat The Stat to increase.
     */
    public StatData(Building building, Stat stat) {
        setData(building, stat);
    }

    public void setData(Building building, Stat stat) {
        mQ = building.getTile().getQ();
        mR = building.getTile().getR();
        mStatID = stat.getID();
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(mQ);
        stream.writeInt(mR);
        stream.writeInt(mStatID);
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        mQ = stream.readInt();
        mR = stream.readInt();
        mStatID = stream.readInt();
    }

    /**
     * Get the {@link HexagonTile} specified.
     *
     * @param map The {@link HexagonMap} being used.
     * @return The specified {@link HexagonMap}, otherwise {@code null}.
     */
    private HexagonTile getTile(HexagonMap map) {
        return map.getTile(mQ, mR);
    }

    /**
     * Get the {@link Building} whose Stat is increasing.
     *
     * @param map The {@link HexagonMap} being used.
     * @return The Building whose stat is increasing, or {@code null}.
     */
    public Building getBuilding(HexagonMap map) {
        if (getTile(map) == null) return null;
        return getTile(map).getBuilding();
    }

    /**
     * Get the {@link Stat} that is being changed.
     *
     * @return The Stat, or {@code null}.
     */
    public Stat getStat() {
        return Stat.getFromID(mStatID);
    }
}
