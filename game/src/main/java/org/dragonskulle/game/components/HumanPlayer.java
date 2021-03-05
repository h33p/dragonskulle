package org.dragonskulle.game.components;

import org.dragonskulle.components.IFrameUpdate;

/**
 * This class will allow a user to interact with game.
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
public class HumanPlayer extends Player implements IFrameUpdate {


    @Override
    protected void onDestroy() {

    }

    @Override
    public void frameUpdate(float deltaTime) {

    }
    
    /**
     * This will take an input from the User and will then change this into a way for the class to understand what the player wants to do
     */
    private void processInput() {
    	
    }

}
