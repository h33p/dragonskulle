package org.dragonskulle.game.player;

// TODO  UPDATE PLAYER STUFF!!!!!!!!!!

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;

/**
 * This base class will allow AI players to be created and used throughout the game.
 * @author Oscar L, Nathaniel Lowis
 */
public class AiPlayer extends Component implements IFixedUpdate, IOnStart {  //TODO remove extends -- Work out whats happening with player
    
	protected float timeSinceStart;
	protected int lowerBoundTime = 5;
	protected int upperBoundTime = 10;
	protected int timeToWait;
	
	protected Reference<Player> player;
	
	protected Random random = new Random();
	
	protected float tileProbability = (float)0.5;
	protected float buildingProabilty = 1 - tileProbability;
	
	protected float upgradeBuilding = (float) 0.2;  // These three probabilities summed must == 1
	protected float attackBuilding = (float) 0.7;
	protected float sellBuilding = (float) 0.1;
		
	/**
	 * A Constructor for an AI Player 
	 */
	public AiPlayer() {}
	
	@Override
	public void onStart() {
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
		
		//Checks to see how long since last time AI player played and if longer than how long they have to wait
		if (timeSinceStart >= timeToWait) {
			timeSinceStart = 0;
			createNewRandomTime();  //Creates new Random Number
			return true;
		}
		
		return false;
	}
	
	/**
	 * This will set how long the AI player has to wait until they can play
	 */
	protected void createNewRandomTime() {
		do {
			
			timeToWait = random.nextInt(upperBoundTime+1);
		} while (timeToWait < lowerBoundTime);
	}
	
	@Override
    protected void onDestroy() {

    }

    @Override
    public void fixedUpdate(float deltaTime) {
    	
    	//updateTokens(deltaTime);
    	if (playGame(deltaTime)) {
    		simulateInput();
    		
    	}
    }

    /**
     * This will simulate the action to be done by the AI player.  For the base class this will be done using probability
     */
    private void simulateInput(){
    	
    	//TODO Need to know how we are interacting with triggerEvent().  Cos here you can choose exact command to do (Much Much easier)
    	
    	if (player.get().numberOfBuildings() == 1) {  //TODO Refactor it so it's only done once
    		
    		List<HexagonTile> tilesToUse = hexTilesToExpand();
    		if (tilesToUse.size() != 0) {
    			int randomIndex = random.nextInt(tilesToUse.size());
    			HexagonTile tileToExpandTo = tilesToUse.get(randomIndex);
    			//now have Hexagon tile to expand to
    			triggerEvent(); //TODO Send data to this which will then package & send to server
    			return;
    		}
    		else {
    			return; //end
    		}
			
    		
    	}
    	else {
    		float randomNumber = random.nextFloat();
    		
    		if (randomNumber <= tileProbability) {
    			List<HexagonTile> tilesToUse = hexTilesToExpand();
        		if (tilesToUse.size() != 0) {
        			int randomIndex = random.nextInt(tilesToUse.size());
        			HexagonTile tileToExpandTo = tilesToUse.get(randomIndex);
        			//now have Hexagon tile to expand to 
        			triggerEvent();  //TODO Send data to this which will then package & send to server
        			return;
        		}
        		else {
        			return; //end
        		}
    		}
    		else {
    			randomNumber = random.nextFloat();
    			
    			
    			if (randomNumber <= upgradeBuilding) {
    				
    				IAmNotABuilding building = player.get().getBuilding(random.nextInt(player.get().numberOfBuildings()));
    				List<Stat> statsArray = building.getStats();
    				Stat statToUpgrade = statsArray.get(random.nextInt(statsArray.size()));
    				triggerEvent();  //TODO Send data to this which will then package & send to server
        			return;
    				
    			}
    			else if (randomNumber > upgradeBuilding && randomNumber <= attackBuilding + upgradeBuilding){
    				ArrayList<IAmNotABuilding[]> buildingsToAttack = new ArrayList<IAmNotABuilding[]>();
    				for (int i = 0; i < player.get().numberOfBuildings(); i++) {
    					IAmNotABuilding building = player.get().getBuilding(i);
    					
    					List<IAmNotABuilding> attackableBuildings = building.attackableBuildings();
    					for (IAmNotABuilding buildingWhichCouldBeAttacked : attackableBuildings) {
    						IAmNotABuilding[] listToAdd = {building, buildingWhichCouldBeAttacked};
    					
    						buildingsToAttack.add(listToAdd);
    					}
    				}
    				
    				if (buildingsToAttack.size() != 0) {
    					IAmNotABuilding[] buildingToAttack = buildingsToAttack.get(random.nextInt(buildingsToAttack.size()));
    					//Chosen building to attack in form [buildingToAttackFrom, buildingToAttack]
    					triggerEvent();  //TODO Send data to this which will then package & send to server
    	    			return;
    				}
    				else {
    					return;
    				}
    				
    				
    			}
    			else {
    				
    				if (player.get().numberOfBuildings() > 1) {
    					IAmNotABuilding buildingToSell = player.getBuilding(random.nextInt(player.get().numberOfBuildings()));
    					//Now have building to sell
    					triggerEvent();  //TODO Send data to this which will then package & send to server
    	    			return;
    				}
    				else {
    					return;
    				}
    				
    				
    			}
    		}
    	}
    }
    
    /**
     * A private method which will return all the hex tiles which can be used to place a building in
     * @return A list of hexagon tiles which can be expanded into.
     */
    private List<HexagonTile> hexTilesToExpand(){
    	List<HexagonTile> hexTilesToExpand = new ArrayList<HexagonTile>();
    	for (int i = 0; i < player.get().numberOfBuildings(); i++) {
    		IAmNotABuilding building = player.get().getBuilding(i);
    		List<HexagonTile> hexTilesWhichCanBeSeen = building.getHexTiles();
    		
    		int r_pos = building.getR();
    		int q_pos = building.getS();
    		
    		for (HexagonTile hexTile: hexTilesWhichCanBeSeen) {
    			   			

    			if (player.get().getHexMap().get(hexTile.getR(), hexTile.getQ()) != null) {

    				; //Ignore cos theres already a building there
    			}
    			else if (!checkCloseBuildings(hexTile)) {  
    				;//IGNORE TILE IT'S WITHIN 1 HEX	
    			}
    			
    			
    			// Can add extra checks here.
    			else {
    				hexTilesToExpand.add(hexTile);
    			}
    		}
    	}
    	return hexTilesToExpand;
    }
    
    /**
     * This will check if the hex tile chosen to build in is within 1 place of any other building.
     * @param hexTile The hex tile to build in
     * @return {@code true} if that hextile is valid to build in or {@code false} if it's not valid 
     */
    private boolean checkCloseBuildings(HexagonTile hexTile) {
    	int r_value = hexTile.getMR();
		int q_value = hexTile.getMQ();
    	int index = 0;
    	boolean validPlace = true;
    	while (validPlace && index < player.get().numberOfBuildings()) { 
			IAmNotABuilding buildingToCheck = player.get().getBuilding(index);
			if ((Math.abs(Math.abs(r_value) - Math.abs(buildingToCheck.getR())) <= 1) && (Math.abs(Math.abs(q_value) - Math.abs(buildingToCheck.getmQ())) <= 1)){
				return false;
			}
			index++;
		}
    	return true;
    }

}
