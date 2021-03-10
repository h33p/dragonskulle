package org.dragonskulle.game.components;

import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.game.input.GameActions;
import org.joml.Vector2d;

/**
 * This class will allow a user to interact with game.
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
public class HumanPlayer extends Player implements IFrameUpdate {

	private Screen screenOn = Screen.MAP_SCREEN;

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
    	if (screenOn == Screen.MAP_SCREEN) {
    		mapScreen();
    	}
    	else if (screenOn == Screen.BUILDING_SCREEN){
    		buildingScreen();
    	}
    	else if (screenOn == Screen.TILE_SCREEN) {
    		expansionScreen();
    	}
    }
    
    private void mapScreen() {
    	if (GameActions.LEFT_CLICK.isActivated()) {
    		Vector2d cursorPosition = GameActions.getCursor().getPosistion();
    		//
    		
    	}
    	
    }
    
    private void buildingScreen() {
    	if (GameActions.LEFT_CLICK.isActivated()) {
    		Vector2d cursorPosition = GameActions.getCursor().getPosistion();
    		
    	}
    }

    private void expansionScreen() {
    	if (GameActions.LEFT_CLICK.isActivated()) {
    		Vector2d cursorPosition = GameActions.getCursor().getPosistion();
    		
    	}
    }
}
