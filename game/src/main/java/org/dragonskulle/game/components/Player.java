package org.dragonskulle.game.components;

import java.util.List;

import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Time;

/**
 * Abstract Player class, Both AIPlayer and HumanPlayer will extend this
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
abstract public class Player extends Component {
	
	protected List<Building> ownedBuildings;
	protected int tokens;  //NEED TO BE SYNCED
	protected final int TOKEN_RATE = 5;
	protected final float UPDATE_TIME = 1;
	protected float lastTokenUpdate = 0;
	protected Reference<HexMap> mapComponent;
	
	/**
	 * This method will take the action decided by the user and will tell the server to perform one action 
	 */
	protected void triggerEvent() {
		;
	}
	
	/**
	 * This method will update the amount of tokens the user has per UPDATE_TIME.  Goes through all owned buildings to check if need to update tokens
	 */
	protected void updateTokens(float time) {
				
		lastTokenUpdate += time;
		if (lastTokenUpdate > UPDATE_TIME) {
			
			//Add tokens
			for (Building building: ownedBuildings) {
				tokens += building.getToken();
				
			}
			tokens += TOKEN_RATE;
			lastTokenUpdate = 0;
		}
	}
}
