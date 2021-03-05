package org.dragonskulle.game.components;

import java.util.Random;

import org.dragonskulle.components.IFixedUpdate;

/**
 * This base class will allow AI players to be created and used throughout the game.
 * @author Oscar L
 */
public class AiPlayer extends Player implements IFixedUpdate {
    
	protected Random random;
	protected float timeSinceStart;
	protected int lowerBoundTime;
	protected int upperBoundTime;
	protected int actualTime;
	
	protected float tileProbablity;
	protected float buildingProabilty = 1 - tileProbablity;
	
	protected float upgradeBuilding;  // These three probabilities summed must == 1
	protected float attackBuilding;
	protected float sellBuilding;
	
	//TODO to choose where to attack, which building to use, what stat to upgrade.  Do we want these to be uniform or not?  I would say it's easier tp be uniform HOWEVER we can play around more easily if they're not uniform
	
	public AiPlayer(int lowerBound, int upperBound) {
		
		//this.super();
		lowerBoundTime = lowerBound;
		upperBoundTime = upperBound;
		
		createNewRandomTime();
		
		
	}
	
	/**
	 * This will check to see whether the AI Player can actually play or not
	 * @param deltaTime The time since the last fixed update
	 * @return A boolean to say whether the AI player can play
	 */
	protected boolean playGame(float deltaTime) {
		timeSinceStart += deltaTime;
		
		if (timeSinceStart >= actualTime) {
			timeSinceStart = 0;
			createNewRandomTime();
			return true;
		}
		
		return false;
	}
	
	/**
	 * This will set how long the AI player has to wait until they can play
	 */
	protected void createNewRandomTime() {
		do {
			
			actualTime = random.nextInt(upperBoundTime+1);
		} while (actualTime < lowerBoundTime);
	}
	
	@Override
    protected void onDestroy() {

    }

    @Override
    public void fixedUpdate(float deltaTime) {
    	if (playGame(deltaTime)) {
    		simulateInput();
    		triggerEvent();
    		
    	}
    }

    /**
     * This will simulate the action to be done by the AI player.  For the base class this will be done using probability
     */
    private void simulateInput(){
    	
    	

    }
}
