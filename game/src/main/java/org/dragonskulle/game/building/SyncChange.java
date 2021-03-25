package org.dragonskulle.game.building;

import java.io.DataInputStream;
import java.io.IOException;

import org.dragonskulle.core.Reference;
import org.dragonskulle.network.components.sync.SyncInt;

import lombok.extern.java.Log;

@Log
public class SyncChange extends SyncInt {

	/** Used to keep track of whether any changes have occurred on the server (causing the local value to become outdated). */
	private transient int mLocalValue = 0;
	
	private transient Reference<Building> mBuilding = new Reference<Building>(null);
	
	public SyncChange() {
		super(0);
	}

	public void setBuilding(Building building) {
		mBuilding = new Reference<Building>(building);
		log.info("mBuilding: " + building);
	}
	
	/**
	 * Flag that change has occurred.
	 */
	public void flagChange() {
		set(get() + 1);
	}
	
	/**
	 * Get whether any changes have been flagged as occurring by the server.
	 * 
	 * @return {@code true} if changes have been made, otherwise {@code false}.
	 */
	public boolean hasChanged() {
		int serverValue = get();
		
		// If the local and server values ae different, something must have updated on the server.
		if(mLocalValue != serverValue) {
			mLocalValue = serverValue;
			return true;
		}
		
		return false;
	}

	@Override
	public void deserialize(DataInputStream in) throws IOException {
		log.info("deserialize in SyncChange.");
		super.deserialize(in);
		
		if(mBuilding == null || mBuilding.isValid() == false) {
			log.info("mBuilding is not valid.");
			return;
		}
		mBuilding.get().repopulateLists();
	}
	
}
