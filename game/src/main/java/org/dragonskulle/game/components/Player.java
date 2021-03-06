package org.dragonskulle.game.components;

import java.util.List;

import org.dragonskulle.components.Component;

/**
 * Abstract Player class, Both AIPlayer and HumanPlayer will extend this
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
abstract public class Player extends Component {
	
	protected List<Building> ownedBuildings;
	/**
	 * This method will take the action decided by the user and will tell the server to perform one action 
	 */
	protected void triggerEvent() {
		
	}
}
