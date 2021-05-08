/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.junit.Test;

/**
 * This will test the upgrade for a basic {@link Building}.
 *
 * @author DragonSkulle
 */
public class UpgradeTest {

    /** This will upgrade one stat to max level. */
    @Test
    public void upgradeOneStat() {
        Building building = new Building();
        building.onConnectedSyncvars();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);

        // Lvl 1
        int lvl1 = attack.getCost();

        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 2
        int lvl2 = attack.getCost();
        assertTrue(lvl2 > lvl1);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 3
        int lvl3 = attack.getCost();
        assertTrue(lvl3 > lvl2);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 4;
        int lvl4 = attack.getCost();
        assertTrue(lvl4 > lvl3);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 5
        int lvl5 = attack.getCost();
        assertTrue(lvl5 > lvl4);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 6
        int lvl6 = attack.getCost();
        assertTrue(lvl6 > lvl5);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 7
        int lvl7 = attack.getCost();
        assertTrue(lvl7 > lvl6);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 8
        int lvl8 = attack.getCost();
        assertTrue(lvl8 > lvl7);
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 9
        int lvl9 = attack.getCost();
        assertTrue(lvl9 > lvl8);
        attack.increaseLevel();
        building.afterStatChange();
    }

    /** Checks if stat cost increases when other stats increase. */
    @Test
    public void inequalAdding() {
        Building building = new Building();
        building.onConnectedSyncvars();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);
        SyncStat defence = stats.get(1);
        SyncStat tokenGeneration = stats.get(2);

        int test1 = defence.getCost();

        // Increase attack lvl
        attack.increaseLevel();
        building.afterStatChange();

        int test2 = defence.getCost();
        assertTrue(test2 > test1);

        // Increase token generation lvl
        tokenGeneration.increaseLevel();
        building.afterStatChange();

        int test3 = defence.getCost();
        assertTrue(test3 > test2);

        // Increase attack lvl
        attack.increaseLevel();
        building.afterStatChange();
        int test4 = defence.getCost();
        assertTrue(test4 > test3);

        // Increase token generation lvl
        tokenGeneration.increaseLevel();
        building.afterStatChange();

        int test5 = defence.getCost();

        assertTrue(test5 > test4);
    }
}
