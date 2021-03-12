/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Random;

import org.dragonskulle.components.Component;
import org.dragonskulle.game.building.stat.AttackStat;
import org.dragonskulle.game.building.stat.DefenceStat;
import org.dragonskulle.game.building.stat.Stat;
import org.dragonskulle.game.building.stat.TokenGenerationStat;
import org.dragonskulle.game.building.stat.ViewDistanceStat;
import org.dragonskulle.game.map.HexagonTile;

@Accessors(prefix = "m")
@Log
public class Building extends Component {

    @Getter private AttackStat mAttack;
    @Getter private DefenceStat mDefence;
    @Getter private TokenGenerationStat mTokenGeneration;
    @Getter private ViewDistanceStat mViewDistance;
    
    private HexagonTile mTile;
    
    public Building(HexagonTile tile) {
    	mTile = tile;
    	
    	mAttack = new AttackStat();
        mDefence = new DefenceStat();
        mTokenGeneration = new TokenGenerationStat();
        mViewDistance = new ViewDistanceStat();
    	
        mAttack.setLevel(5);
        mDefence.setLevel(5);
        mTokenGeneration.setLevel(5);
        mViewDistance.setLevel(5);
        
        /*
        String l0 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l1 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l2 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l3 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l4 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l5 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        
        log.info(
        		l0 + l1 + l2 + l3 + l4 + l5
        );
        */
        
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
    		log.info(String.format("Failed attack: random number %f was greater or equal to target %f.", successChance, target));
    		return false;
    	}
    	
    }
    
    public ArrayList<Building> getAttackableBuildings() {
    	ArrayList<Building> buildings = new ArrayList<Building>();
    	
    	mTile.getQ();
    	
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
