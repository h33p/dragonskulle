/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertTrue;

import org.dragonskulle.core.Scene;
import org.dragonskulle.core.Scene.SceneOverride;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This will test the upgrade for a basic {@link Building}.
 *
 * @author DragonSkulle
 */
public class UpgradeTest {

    private SceneOverride mOverride;
    private Building mBuilding;

    @Before
    public void setup() {
        Scene scene = new Scene("");

        scene.registerSingleton(new GameState());

        mOverride = new SceneOverride(scene);
        mBuilding = new Building();
        mBuilding.onConnectedSyncvars();
    }

    /** This will upgrade one stat to max level checking cost is always increasing */
    @Test
    public void upgradeOneStat() {

        SyncStat attack = mBuilding.getStat(StatType.ATTACK);

        int i = 1;
        // Test all to max
        while (!attack.isMaxLevel()) {

            int pre = attack.getCost();
            attack.increaseLevel();
            mBuilding.afterStatChange();
            int after = attack.getCost();
            assertTrue("This has failed on loop" + i, after > pre);
            i++;
        }
    }

    /** Checks if stat cost increases when other stats increase. */
    @Test
    public void inequalAdding() {

        SyncStat attack = mBuilding.getStat(StatType.ATTACK);
        SyncStat defence = mBuilding.getStat(StatType.ATTACK);
        SyncStat tokenGeneration = mBuilding.getStat(StatType.TOKEN_GENERATION);

        int test1 = defence.getCost();

        // Increase attack lvl
        attack.increaseLevel();
        mBuilding.afterStatChange();

        int test2 = defence.getCost();
        assertTrue(test2 > test1);

        // Increase token generation lvl
        tokenGeneration.increaseLevel();
        mBuilding.afterStatChange();

        int test3 = defence.getCost();
        assertTrue(test3 > test2);

        // Increase attack lvl
        attack.increaseLevel();
        mBuilding.afterStatChange();
        int test4 = defence.getCost();
        assertTrue(test4 > test3);

        // Increase token generation lvl
        tokenGeneration.increaseLevel();
        mBuilding.afterStatChange();

        int test5 = defence.getCost();

        assertTrue(test5 > test4);
    }

    @After
    public void cleanup() {
        mOverride.close();
    }
}
