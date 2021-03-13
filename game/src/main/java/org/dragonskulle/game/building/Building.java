/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Random;

import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.AttackDistanceStat;
import org.dragonskulle.game.building.stat.AttackStat;
import org.dragonskulle.game.building.stat.DefenceStat;
import org.dragonskulle.game.building.stat.Stat;
import org.dragonskulle.game.building.stat.TokenGenerationStat;
import org.dragonskulle.game.building.stat.ViewDistanceStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;

@Accessors(prefix = "m")
@Log
public class Building extends Component {

    @Getter private AttackStat mAttack;
    @Getter private DefenceStat mDefence;
    @Getter private TokenGenerationStat mTokenGeneration;
    @Getter private ViewDistanceStat mViewDistance;
    @Getter private AttackDistanceStat mAttackDistance;
    
    /** The owner of the building. */
    @Getter @Setter private Reference<TestPlayer> mOwner = new Reference<TestPlayer>(null);
    /** The HexagonTile the building is on. */
    private Reference<HexagonTile> mTile = new Reference<HexagonTile>(null);
    /** The HexagonMap being used. */
    private Reference<HexagonMap> mHexagonMap = new Reference<HexagonMap>(null);
    
    /**
     * Create a new {@link Building}. Adds the Building to the {@link HexagonMap} at the specified {@link HexagonTile}.
     * 
     * @param hexagonMap The HexagonMap being used.
     * @param hexagonTile The HexagonTile the building is on.
     */
    public Building(Reference<HexagonMap> hexagonMap, Reference<HexagonTile> hexagonTile) {
    	mTile = hexagonTile;
    	mHexagonMap = hexagonMap;
    	
    	mAttack = new AttackStat();
        mDefence = new DefenceStat();
        mTokenGeneration = new TokenGenerationStat();
        mViewDistance = new ViewDistanceStat();
        mAttackDistance = new AttackDistanceStat();
    	
        mAttack.setLevel(5);
        mDefence.setLevel(5);
        mTokenGeneration.setLevel(5);
        mViewDistance.setLevel(5);
        mAttackDistance.setLevel(5);
        
        // Move out of constructor:
        HexagonMap map = hexagonMap.get();
    	HexagonTile tile = mTile.get();
    	if(map != null && tile != null) {
    		map.storeBuilding(this, tile.getQ(), tile.getR());
    	}
        
    }
    
    /**
     * TODO: Currently not fully functional.
     * Doesn't currently have impact on anything. 
     * 
     * @param opponent
     * @return Whether the attack is successful.
     */
    public boolean attack(Building opponent) {
    	Random random = new Random();
    	double successChance = random.nextDouble();
    	
    	//double target = mAttack.getValue() - opponent.getDefence().getValue();
    	double target = 0.5;
    	
    	if(successChance >= target) {
    		//log.info(String.format("Successful attack: random number %f was greater or equal to target %f (target calculated using %f and %f).", successChance, target, mAttack.getValue(), opponent.getDefence().getValue()));
    		log.info(String.format("Successful attack: random number %f was greater or equal to target %f.", successChance, target));
    		return true;
    	} else {
    		//log.info(String.format("Failed attack: random number %f was not greater or equal to target %f (target calculated using %f and %f).", successChance, target, mAttack.getValue(), opponent.getDefence().getValue()));
    		log.info(String.format("Failed attack: random number %f was not greater or equal to target %f.", successChance, target));
    		return false;
    	}
    	
    }
    
    /**
     * Get an ArrayList of {@link HexagonTile}s that are within the Building's view range, as specified by {@link #mViewDistance}.
     * 
     * @return All the HexagonTiles within the building's view range, excluding the Building's HexagonTile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getViewTiles() {
    	// Get the current view distance.
    	int distance = mViewDistance.getValue();
    	// Get the tiles within the view distance.
    	return getTilesInRadius(distance);
    }
    
    /**
     * Get an ArrayList of {@link HexagonTile}s that are within the Building's attack range, as specified by {@link #mAttackDistance}.
     * 
     * @return All the HexagonTiles within the building's attack range, excluding the Building's HexagonTile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getAttackTiles() {
    	// Get the current view distance.
    	int distance = mAttackDistance.getValue();
    	// Get the tiles within the view distance.
    	return getTilesInRadius(distance);
    }
    
    /**
     * Get an ArrayList of all {@link HexagonTile}s, excluding the building's HexagonTile, within a set radius.
     * 
     * @param radius The radius.
     * @return An ArrayList of HexgaonTiles, otherwise an empty ArrayList.
     */
    private ArrayList<HexagonTile> getTilesInRadius(int radius){
    	ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();
    	
    	// Attempt to get the current HexagonTile and HexagonMap.
    	HexagonTile tile = mTile.get();
    	HexagonMap map = mHexagonMap.get();
    	if(tile == null || map == null) return tiles;
    	
    	// Get the current q and r coordinates.
    	int qCentre = tile.getQ();
    	int rCentre = tile.getR();
    	
    	for(int rOffset = -radius; rOffset <= radius; rOffset++) {
    		for(int qOffset = -radius; qOffset <= radius; qOffset++) {
    			// Only get tiles whose s coordinates are within the desired range.
    			int s = -qOffset - rOffset;
    			if(s > radius || s < -radius) {
    				//log.info(String.format("INVALID S: q = %d, r = %d, s = %d ", qOffset, rOffset, s));
    				continue;
    			}
    			
    			// Do not include the building's HexagonTile.
    			if(qOffset == 0 && rOffset == 0) {
    				//log.info(String.format("Current tile: q = %d, r = %d, s = %d ", qOffset, rOffset, s));
    				continue;
    			}
    			
    			//log.info(String.format("q = %d, r = %d, s = %d ", qOffset, rOffset, s));
    			
    			// Attempt to get the desired tile, and check if it exists.
    			HexagonTile selectedTile = map.getTile(qCentre + qOffset, rCentre + rOffset);
    			if(selectedTile == null) {
    				continue;
    			}
    			
    			// Add the tile to the list.
    			tiles.add(selectedTile);    			
    		}
    	}
    	
    	// log.info("Number of tiles in range: " + tiles.size());
    	
    	return tiles;
    }
    
    // TODO: Add ownership
    public ArrayList<Building> getAttackableBuildings() {
    	ArrayList<Building> buildings = new ArrayList<Building>();
    	
    	HexagonMap map = mHexagonMap.get();
    	TestPlayer owner = mOwner.get();
    	if(map == null || owner == null) return buildings;
    	
    	ArrayList<HexagonTile> attackTiles = getAttackTiles();
    	for (HexagonTile tile : attackTiles) {
			Building building = map.getBuilding(tile.getQ(), tile.getR());
			if(building == null) {
				continue;
			}
			
			Reference<TestPlayer> buildingOwner = building.getOwner();
			if(owner.equals(buildingOwner.get())) {
				continue;
			}
			
			buildings.add(building);
		}
    	
    	return buildings;
    }
    
    /**
     * TODO: Remove. This should be accessed by {@code myBuilding.getTokenGeneration().getValue() }
     * @return The number of tokens that should be generated by the building.
     */
    public int getToken(){
    	return mTokenGeneration.getValue();
    }

    /**
     * Get an ArrayList of Stats that the Building has.
     * 
     * @return An ArrayList of Stats.
     */
    public ArrayList<Stat<?>> getStats(){
    	ArrayList<Stat<?>> stats = new ArrayList<Stat<?>>();
    	stats.add(mAttack);
    	stats.add(mDefence);
    	stats.add(mTokenGeneration);
    	stats.add(mViewDistance);
    	
    	return stats;
    }
    
    @Override
    protected void onDestroy() {}
}
