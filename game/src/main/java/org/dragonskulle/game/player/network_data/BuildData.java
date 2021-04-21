/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.network_data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * The Class which holds the data for building a building.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
public class BuildData implements INetSerializable {

    private int mQ;
    private int mR;
    @Setter private int mDescriptorIndex;

    public BuildData() {}

    /**
     * The constructor.
     *
     * @param hexTileToAdd The {@code HexagonTile} to build on
     */
    public BuildData(HexagonTile hexTileToAdd, int descriptorIndex) {
        this.mDescriptorIndex = descriptorIndex;
        setTile(hexTileToAdd);
    }

    public void setTile(HexagonTile hexTileToAdd) {
        mQ = hexTileToAdd.getQ();
        mR = hexTileToAdd.getR();
    }

    @Override
    public void serialize(DataOutputStream stream, int clientId) throws IOException {
        stream.writeInt(mQ);
        stream.writeInt(mR);
        stream.writeInt(mDescriptorIndex);
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        mQ = stream.readInt();
        mR = stream.readInt();
        mDescriptorIndex = stream.readInt();
    }

    public HexagonTile getTile(HexagonMap map) {
        return map.getTile(mQ, mR);
    }

    public BuildingDescriptor getDescriptor() {
        return PredefinedBuildings.get(mDescriptorIndex);
    }
}
