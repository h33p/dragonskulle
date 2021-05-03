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

        assertEquals(1, actualBuildings.get(0).getCost());
        assertEquals(10, actualBuildings.get(1).getCost());
        assertEquals(30, actualBuildings.get(2).getCost());
        assertEquals(31, actualBuildings.get(3).getCost());
        assertEquals(32, actualBuildings.get(4).getCost());
        assertEquals(33, actualBuildings.get(5).getCost());
        assertEquals(25, actualBuildings.get(6).getCost());
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

        assertEquals(27, building.getAttackCost());
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);
        attack.increaseLevel();

        building.afterStatChange();
        assertEquals(29, building.getAttackCost());

        attack.increaseLevel();
        attack.increaseLevel();

        SyncStat defence = stats.get(1);

        defence.increaseLevel();
        defence.increaseLevel();

        building.afterStatChange();

        assertEquals(39, building.getAttackCost());
    }

    /** This will test that the cost increases when stats are upgraded */
    @Test
    public void upgradeStats() {
        Building building = new Building();
        building.onConnectedSyncvars();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        assertEquals(1, stats.get(0).getCost());
        stats.get(0).increaseLevel();
        building.afterStatChange();
        assertEquals(2, stats.get(0).getCost());
        stats.get(1).increaseLevel();
        stats.get(1).increaseLevel();
        building.afterStatChange();
        assertEquals(3, stats.get(0).getCost());
    }
}