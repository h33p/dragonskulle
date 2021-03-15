package org.dragonskulle.game.player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.Stat;
import org.dragonskulle.network.components.sync.INetSerializable;

public class StatData implements INetSerializable {
	
	private Building mBuilding;
	private Stat<?> mStat;
	
	public StatData() {}
	
	public StatData(Building building, Stat<?> stat) {
		mBuilding = building;
		mStat = stat;
	}

	@Override
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeInt(mBuilding.getTile().get().getQ());
		stream.writeInt(mBuilding.getTile().get().getR());
		stream.writeInt(mBuilding.getTile().get().getS());
		
		
		
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	

}
