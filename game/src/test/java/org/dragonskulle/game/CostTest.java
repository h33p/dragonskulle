/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.junit.Test;

/**
 * Will test the cost for buildings
 *
 * @author DragonSkulle
 */
public class CostTest {

    /** This will check that there is different costs for different predefined Buildings */
    @Test
    public void buildingCost() {
        List<BuildingDescriptor> actualBuildings = PredefinedBuildings.getAll();

        // Camp
        assertEquals(20, actualBuildings.get(0).getCost());
        
        // Town
        assertEquals(75, actualBuildings.get(1).getCost());
        
        // City
        assertEquals(200, actualBuildings.get(2).getCost());
        
        // Barracks
        assertEquals(225, actualBuildings.get(3).getCost());
        
        // Castle
        assertEquals(225, actualBuildings.get(4).getCost());
        
        // Merchants
        assertEquals(225, actualBuildings.get(5).getCost());
        
        // Military Complex
        assertEquals(500, actualBuildings.get(6).getCost());
        
        // Fortress
        assertEquals(500, actualBuildings.get(7).getCost());
        
        // Guild
        assertEquals(500, actualBuildings.get(8).getCost());
    }

    /** This will check that the sell price is right */
    @Test
    public void sellCost() {
        List<BuildingDescriptor> actualBuildings = PredefinedBuildings.getAll();

        assertEquals(1, actualBuildings.get(0).getSellPrice());
        assertEquals(2, actualBuildings.get(1).getSellPrice());
        assertEquals(2, actualBuildings.get(2).getSellPrice());
        assertEquals(2, actualBuildings.get(3).getSellPrice());
        assertEquals(2, actualBuildings.get(4).getSellPrice());
        assertEquals(2, actualBuildings.get(5).getSellPrice());
        assertEquals(2, actualBuildings.get(6).getSellPrice());
    }

    /** This will test attacking cost */
    @Test
    public void attackCost() {
        Building building = new Building();
        building.onConnectedSyncvars();
        building.afterStatChange();

        // Lvl 1 Attack Lvl 1 Defence Lvl 1 TGen
        assertEquals(27, building.getAttackCost());
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);
        attack.increaseLevel();

        building.afterStatChange();

        // Lvl 2 Attack Lvl 1 Defence Lvl 1 TGen
        assertEquals(30, building.getAttackCost());

        attack.increaseLevel();
        attack.increaseLevel();

        SyncStat defence = stats.get(1);

        defence.increaseLevel();
        defence.increaseLevel();

        building.afterStatChange();

        // Lvl 4 Attack Lvl 3 Defence Lvl 1 TGen
        assertEquals(42, building.getAttackCost());

        building.setCapital(true);

        // Lvl 4 Attack Lvl 3 Defence Lvl 1 TGen Also Capital
        assertEquals(52, building.getAttackCost());
    }

    /** This will test that the cost increases when stats are upgraded */
    @Test
    public void upgradeStats() {
        Building building = new Building();

        building.onConnectedSyncvars();
        building.afterStatChange();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        // Lvl 1 Attack Lvl 1 Defence Lvl 1 TGen
        assertEquals(4, stats.get(0).getCost());
        stats.get(0).increaseLevel();
        building.afterStatChange();

        // Lvl 2 Attack Lvl 1 Defence Lvl 1 TGen
        assertEquals(9, stats.get(0).getCost());
        stats.get(1).increaseLevel();
        stats.get(1).increaseLevel();
        building.afterStatChange();

        // Lvl 2 Attack Lvl 3 Defence Lvl 1 TGen
        assertEquals(18, stats.get(0).getCost());
    }
}
