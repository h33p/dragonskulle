/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.junit.Test;

/**
 * Will test the cost for buildings
 *
 * @author DragonSkulle
 */
public class CostTest {

    /**
     * This will check that there is different stats and costs for a made up predefined Buildings
     */
    @Test
    public void buildingCost() {
        BuildingDescriptor building =
                new BuildingDescriptor(3, 7, 2, 75, 25, "ui/2_stars.png", "Town");

        assertEquals(3, building.getAttack());
        assertEquals(7, building.getDefence());
        assertEquals(2, building.getTokenGenerationLevel());
        assertEquals(75, building.getCost());
        assertEquals(25, building.getSellPrice());
    }

    /** This will test attacking cost */
    @Test
    public void attackCost() {
        Building building = new Building();
        building.onConnectedSyncvars();
        building.afterStatChange();

        // Lvl 1 Attack Lvl 1 Defence Lvl 1 TGen
        int lvl1All = building.getAttackCost();

        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);
        attack.increaseLevel();

        building.afterStatChange();

        int lvl2Attack = building.getAttackCost();
        assertTrue(lvl2Attack > lvl1All);

        SyncStat defence = stats.get(1);

        defence.increaseLevel();
        defence.increaseLevel();

        building.afterStatChange();

        int upgradeDefence = building.getAttackCost();

        assertTrue(upgradeDefence > lvl2Attack);

        building.setCapital(true);

        int setCapital = building.getAttackCost();

        assertTrue(setCapital > upgradeDefence);
    }

    /** This will test that the cost increases when stats are upgraded */
    @Test
    public void upgradeStats() {
        Building building = new Building();

        building.onConnectedSyncvars();
        building.afterStatChange();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        // Lvl 1 Attack Lvl 1 Defence Lvl 1 TGen
        int lvl1All = stats.get(0).getCost();
        stats.get(0).increaseLevel();
        building.afterStatChange();

        // Lvl 2 Attack Lvl 1 Defence Lvl 1 TGen
        int lvl2Attack = stats.get(0).getCost();
        assertTrue(lvl2Attack > lvl1All);
        stats.get(1).increaseLevel();
        stats.get(1).increaseLevel();
        building.afterStatChange();

        // Lvl 2 Attack Lvl 3 Defence Lvl 1 TGen
        int lvl3Defence = stats.get(0).getCost();
        assertTrue(lvl3Defence > lvl2Attack);
    }
}
