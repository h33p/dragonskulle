/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.network_data;

import java.io.DataInput;
import java.io.DataOutput;
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

    /** Constructor. */
    public BuildData() {}

    /**
     * The constructor.
     *
     * @param hexTileToAdd The {@link HexagonTile} to build on
     * @param descriptorIndex The index of the relevant {@link BuildingDescriptor}.
     */
    public BuildData(HexagonTile hexTileToAdd, int descriptorIndex) {
        setTile(hexTileToAdd, descriptorIndex);
    }

    /**
     * Set the contents of the BuildData.
     *
     * @param hexTileToAdd The hexagon tile.
     * @param descriptorIndex The type of building it should be.
     */
    public void setTile(HexagonTile hexTileToAdd, int descriptorIndex) {
        mQ = hexTileToAdd.getQ();
        mR = hexTileToAdd.getR();
        mDescriptorIndex = descriptorIndex;
    }

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
        stream.writeInt(mQ);
        stream.writeInt(mR);
        stream.writeInt(mDescriptorIndex);
    }

    @Override
    public void deserialize(DataInput stream) throws IOException {
        mQ = stream.readInt();
        mR = stream.readInt();
        mDescriptorIndex = stream.readInt();
    }

    /**
     * Gets the tile the data is about from the map using its coordinates.
     *
     * @param map the map
     * @return the {@link HexagonTile}
     */
    public HexagonTile getTile(HexagonMap map) {
        return map.getTile(mQ, mR);
    }

    /**
     * Gets the {@code BuildingDescriptor} from it's index.
     *
     * @return the descriptor
     */
    public BuildingDescriptor getDescriptor() {
        return PredefinedBuildings.get(mDescriptorIndex);
    }
}
