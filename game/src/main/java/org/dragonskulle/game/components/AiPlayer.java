package org.dragonskulle.game.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.core.Reference;

/**
 * This base class will allow AI players to be created and used throughout the game.
 * @author Oscar L
 */
public class AiPlayer extends Player implements IFixedUpdate {
    
	protected float timeSinceStart;
	protected int lowerBoundTime;
	protected int upperBoundTime;
	protected int actualTime;
	
	protected Random random = new Random();
	
	protected float tileProbability = (float)0.5;
	protected float buildingProabilty = 1 - tileProbability;
	
	protected float upgradeBuilding = (float) 0.2;  // These three probabilities summed must == 1
	protected float attackBuilding = (float) 0.7;
	protected float sellBuilding = (float) 0.1;
	
	//TODO to choose where to attack, which building to use, what stat to upgrade.  Do we want these to be uniform or not?  I would say it's easier to be uniform HOWEVER we can play around more easily if they're not uniform
	
	/**
	 * A Constructor for an AI Player 
	 * @param lowerBound the lower bound in time for how often you want the AI player to play
	 * @param upperBound the upper bound in time for how often you want the AI player to play
	 * @param map the map being used for this game
	 * @param capital the capital used by the player
	 * 
	 * <p> So if you set the lowerBound to 2 and upperBound the AI Player will have a go every 2 - 5 seconds (Depending on the random number picked) <\p>
	 */
	public AiPlayer(int lowerBound, int upperBound, Reference<HexMap> map, Building capital) {
		
		super(map, capital);
		lowerBoundTime = lowerBound;
		upperBoundTime = upperBound;
		timeSinceStart = 0;
		
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
    	updateTokens(deltaTime);
    	if (playGame(deltaTime)) {
    		simulateInput();
    		triggerEvent();
    		
    	}
    }

    /**
     * This will simulate the action to be done by the AI player.  For the base class this will be done using probability
     */
    private void simulateInput(){
    	
    	//TODO Need to know how we are interacting with triggerEvent().  Cos here you can choose exact command to do (Much Much easier)
    	//TODO ATM have the set up probabilties to choose which event to do.  Need to add which building/tile to use
    	
    	if (ownedBuildings.size() == 1) {
    		//TODO Choose which tile to use;
    		return;
    	}
    	else {
    		float randomNumber = random.nextFloat();
    		
    		if (randomNumber <= tileProbability) {
    			//TODO Choose which tile to use
    			//Need way to choose which tile to use -- Guessing best way is to use Building again
    			return;
    		}
    		else {
    			randomNumber = random.nextFloat();
    			Building building = ownedBuildings.get(random.nextInt(ownedBuildings.size()));
    			
    			if (randomNumber <= upgradeBuilding) {
    				//TODO Choose which building to upgrade & which stat to upgrade
    				return;
    			}
    			else if (randomNumber > upgradeBuilding && randomNumber <= attackBuilding + upgradeBuilding){
    				//TODO Choose which building to attack
    				ArrayList<Building[]> buildingsToAttack = new ArrayList<Building[]>();
    				for (Building building: ownedBuildings) {
    					
    					List<Building> attackableBuilding = building.attackableBuildings();
    					Building[] listToAdd = {building, attackableBuilding};
    					
    					buildingsToAttack.add(listToAdd);
    				}
    				
    				Building[] buildingToAttack = buildingsToAttack.get(random.nextInt(buildingsToAttack.size()));
    				
    				//Chosen building to attack in form [buildingToAttackFrom, buildingToAttack]
    			}
    			else {
    				//TODO Choose which building to sell
    				return;
    			}
    		}
    	}
    }
    
    private List<HexTiles> hexTilesToExpand(){
    	List<HexTiles> hexTilesToExpand = new ArrayList<HexTiles>();
    	for (Building building: ownedBuildings) {
    		;
    	}
    }
}
