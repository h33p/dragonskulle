package org.dragonskulle.game.components;

import org.dragonskulle.components.Component;

/**
 * Abstract Player class, Both AIPlayer and HumanPlayer will extend this
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
abstract public class Player extends Component {
	
	/**
	 * This method will take the action decided by the user and will tell the server to perform one action 
	 */
	protected void triggerEvent() {
		
	}
}
