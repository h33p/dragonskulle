/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;

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

    /** This checks the starting value */
    @Test
    public void basicTest() {
        Building building = new Building();
        building.onConnectedSyncvars();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        assertEquals(4, stats.get(0).getCost());
        assertEquals(4, stats.get(1).getCost());
        assertEquals(4, stats.get(2).getCost());
    }

    /** This will upgrade one stat to max level. */
    @Test
    public void upgradeOneStat() {
        Building building = new Building();
        building.onConnectedSyncvars();
        ArrayList<SyncStat> stats = building.getUpgradeableStats();

        SyncStat attack = stats.get(0);

        // Lvl 1
        assertEquals(1, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 2
        assertEquals(3, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 3
        assertEquals(6, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 4;
        assertEquals(9, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 5
        assertEquals(12, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 6
        assertEquals(15, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 7
        assertEquals(18, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 8
        assertEquals(21, attack.getCost());
        attack.increaseLevel();
        building.afterStatChange();

        // Lvl 9
        assertEquals(24, attack.getCost());
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

        assertEquals(1, attack);
        assertEquals(1, defence);
        assertEquals(1, tokenGeneration);

        // Increase attack lvl
        attack.increaseLevel();
        building.afterStatChange();

        assertEquals(4, defence.getCost());

        // Increase token generation lvl
        tokenGeneration.increaseLevel();
        building.afterStatChange();

        assertEquals(8, defence.getCost());

        // Increase attack lvl
        attack.increaseLevel();
        building.afterStatChange();
        assertEquals(8, defence.getCost());

        // Increase token generation lvl
        tokenGeneration.increaseLevel();
        building.afterStatChange();

        assertEquals(8, defence.getCost());
    }
}
