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

        assertEquals(1, stats.get(0).getCost());
        assertEquals(1, stats.get(1).getCost());
        assertEquals(1, stats.get(2).getCost());
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

        // Lvl 2
        attack.increaseLevel();
        assertEquals(3, attack.getCost());

        // Lvl 3
        attack.increaseLevel();
        assertEquals(6, attack.getCost());

        // Lvl 4
        attack.increaseLevel();
        assertEquals(9, attack.getCost());

        // Lvl 5
        attack.increaseLevel();
        assertEquals(12, attack.getCost());

        // Lvl 6
        attack.increaseLevel();
        assertEquals(15, attack.getCost());

        // Lvl 7
        attack.increaseLevel();
        assertEquals(18, attack.getCost());

        // Lvl 8
        attack.increaseLevel();
        assertEquals(21, attack.getCost());

        // Lvl 9
        attack.increaseLevel();
        assertEquals(24, attack.getCost());
    }
}
