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

    /** This will upgrade one stat to max level checking cost is always increasing */
    @Test
    public void upgradeOneStat() {
        Building building = new Building();
        building.onConnectedSyncvars();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);

        int i = 1;
        // Test all to max
        while (!attack.isMaxLevel()) {

            int pre = attack.getCost();
            attack.increaseLevel();
            building.afterStatChange();
            int after = attack.getCost();
            assertTrue("This has failed on loop" + i, after > pre);
            i++;
        }
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
