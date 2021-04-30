/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;

import java.util.List;
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
}
