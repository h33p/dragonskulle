/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Random;

import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
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
    
    private Reference<HexagonTile> mTile;
    private Reference<HexagonMap> mHexagonMap;
    
    public Building(Reference<HexagonMap> hexagonMap, Reference<HexagonTile> hexagonTile) {
    	mTile = hexagonTile;
    	mHexagonMap = hexagonMap;
    	
    	mAttack = new AttackStat();
        mDefence = new DefenceStat();
        mTokenGeneration = new TokenGenerationStat();
        mViewDistance = new ViewDistanceStat();
    	
        mAttack.setLevel(5);
        mDefence.setLevel(5);
        mTokenGeneration.setLevel(5);
        mViewDistance.setLevel(5);
        
        // Move out of constructor;
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
    
    public ArrayList<HexagonTile> getViewTiles() {
    	ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();
    	
    	HexagonTile tile = mTile.get();
    	HexagonMap map = mHexagonMap.get();
    	if(tile == null || map == null) return tiles;
    	
    	
    	int distance = mViewDistance.getValue();
    	int qCentre = tile.getQ();
    	int rCentre = tile.getR();
    	
    	int i = 0;
    	
    	for(int r = -distance; r <= distance; r++) {
    		for(int q = -distance; q <= distance; q++) {
    			int s = -q - r;
    			if(Math.abs(s) > distance) {
    				log.info(String.format("INVALID S: q = %d, r = %d, s = %d ", q, r, s));
    				continue;
    			}
    			
    			if(q == 0 && r == 0) {
    				log.info(String.format("Current tile: q = %d, r = %d, s = %d ", q, r, s));
    				continue;
    			}
    			
    			log.info(String.format("q = %d, r = %d, s = %d ", q, r, s));
    			i++;
    			HexagonTile selectedTile = map.getTile(qCentre + q, rCentre + r);
    			if(selectedTile == null) {
    				log.info("TILE IS NULL");
    				continue;
    			}
    			
    			tiles.add(selectedTile);
    			
    		}
    		
    	}
    	
    	log.info("i = " + i);
    	
    	
    	return tiles;
    }
    
    public ArrayList<Building> getAttackableBuildings() {
    	ArrayList<Building> buildings = new ArrayList<Building>();
    	
    	HexagonTile tile = mTile.get();
    	if(tile == null) return null;
    	tile.getQ();
    	
    	return buildings;
    }
    
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
