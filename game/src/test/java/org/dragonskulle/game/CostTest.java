/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import lombok.extern.java.Log;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.Scene.SceneOverride;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Will test the cost for buildings.
 *
 * @author DragonSkulle
 */
@Log
public class CostTest {

    SceneOverride mOverride;
    Building mBuilding;

    @Before
    public void setup() {
        Scene scene = new Scene("");

        scene.registerSingleton(new GameState());

        mOverride = new SceneOverride(scene);
        mBuilding = new Building();
        mBuilding.onConnectedSyncvars();
    }

    /**
     * This will check that there is different stats and costs for a made up predefined Buildings.
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

    /** This will test attacking cost. */
    @Test
    public void attackCost() {

        // Lvl 1 Attack Lvl 1 Defence Lvl 1 TGen
        int lvl1All = mBuilding.getAttackCost();

        SyncStat attack = mBuilding.getStat(StatType.ATTACK);
        attack.increaseLevel();

        mBuilding.afterStatChange();

        int lvl2Attack = mBuilding.getAttackCost();
        assertTrue(lvl2Attack > lvl1All);

        SyncStat defence = mBuilding.getStat(StatType.DEFENCE);

        defence.increaseLevel();
        defence.increaseLevel();

        mBuilding.afterStatChange();

        int upgradeDefence = mBuilding.getAttackCost();

        assertTrue(upgradeDefence > lvl2Attack);

        mBuilding.setCapital(true);

        int setCapital = mBuilding.getAttackCost();

        assertTrue(setCapital > upgradeDefence);
    }

    /** This will test that the cost increases when stats are upgraded. */
    @Test
    public void upgradeStats() {

        // Lvl 1 Attack Lvl 1 Defence Lvl 1 TGen

        int lvl1All = mBuilding.getStat(StatType.ATTACK).getCost();

        mBuilding.getStat(StatType.ATTACK).increaseLevel();
        mBuilding.afterStatChange();

        // Lvl 2 Attack Lvl 1 Defence Lvl 1 TGen
        int lvl2Attack = mBuilding.getStat(StatType.ATTACK).getCost();
        assertTrue(lvl2Attack > lvl1All);

        mBuilding.getStat(StatType.DEFENCE).increaseLevel();
        mBuilding.getStat(StatType.DEFENCE).increaseLevel();
        mBuilding.afterStatChange();

        // Lvl 2 Attack Lvl 3 Defence Lvl 1 TGen
        int lvl3Defence = mBuilding.getStat(StatType.ATTACK).getCost();
        assertTrue(lvl3Defence > lvl2Attack);
    }

    @After
    public void cleanup() {
        mOverride.close();
    }
}
