package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Time;
import org.dragonskulle.game.map.HexagonMap;

import lombok.Getter;

/**
 * Abstract Player class, Both AIPlayer and HumanPlayer will extend this
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
public class Player {		
	
	private List<Building> ownedBuildings;
	private Reference<HexagonMap> mapComponent;
	@Getter private final int UNIQUE_ID;	
	private static int nextID;
	
	@Getter private int tokens = 0;  //TODO NEED TO BE SYNCint		
	private final int TOKEN_RATE = 5;
	private final float UPDATE_TIME = 1;
	private float lastTokenUpdate = 0;
	
	
	/**
	 * The base constructor for player 
	 * @param map the map being used for this game
	 * @param capital the capital used by the player
	 */
	public Player(Reference<HexagonMap> map, Building capital) {		//TODO DO we need?
		UNIQUE_ID = 5;  			//TODO need to make this static so unique for each player
		mapComponent = map;
		ownedBuildings = new ArrayList<Building>();
		ownedBuildings.add(capital);
		updateTokens(UPDATE_TIME + 1);
	}
	
	public void addBuilding(Building building) {
		ownedBuildings.add(building);
	}
	
	public Building getBuilding(int index) {
		return ownedBuildings.get(index);
	}
	
	public int numberOfBuildings() {
		return ownedBuildings.size();
	}
	
	public void removeBuilding(Building building) {
		ownedBuildings.remove(building);
	}
	
	public Reference<HexagonMap> getHexMap() {
		return mapComponent;
	}
	/**
	 * This method will update the amount of tokens the user has per UPDATE_TIME.  Goes through all owned buildings to check if need to update tokens
	 */
	public void updateTokens(float time) {  //TODO move this to server once server integrated.
			
		
		lastTokenUpdate += time;
		//Checks to see how long its been since lastTokenUpdate 
		if (lastTokenUpdate > UPDATE_TIME) {
			
			//Add tokens for each building
			for (Building building: ownedBuildings) {
				tokens += building.getToken();
				
			}
			//Add final tokens
			tokens += TOKEN_RATE;
			lastTokenUpdate = 0;
		}
	}

}
