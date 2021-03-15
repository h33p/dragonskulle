package org.dragonskulle.game.player.networkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * 
 * @author Dragonskulle
 *
 */
public class BuildData implements INetSerializable {
	
	private HexagonTile mHexTile;
	
	public BuildData() {}
	
	public BuildData(HexagonTile hexTileToAdd) {
		mHexTile = hexTileToAdd;
	}

	@Override
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeInt(mHexTile.getQ());
		stream.writeInt(mHexTile.getR());
		stream.writeInt(mHexTile.getS());
		
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		mHexTile = new HexagonTile(stream.readInt(), stream.readInt(), stream.readInt());
		
	}

}
