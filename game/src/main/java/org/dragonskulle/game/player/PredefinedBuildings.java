/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Collection of {@code BuildingDescriptor}'s.
 *
 * @author Oscar L
 */
public class PredefinedBuildings {
    /**
     * The predefined buildings.
     */
    private static final List<BuildingDescriptor> buildings =
            new ArrayList<>(
                    Arrays.asList(
                            new BuildingDescriptor(1, 1, 1, 1, 1, 10, 2, null),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 30, 2, null),
                            new BuildingDescriptor(1, 1, 100, 1, 1, 25, 2, null)));

    /**
     * Gets all the buildings which can be placed filtered by the cost of the building.
     *
     * @param currentTokens the current tokens the player has
     * @return a list of purchasable buildings
     */
    public static List<BuildingDescriptor> getPurchasable(int currentTokens) {
        return buildings.stream()
                .filter(b -> b.getCost() <= currentTokens)
                .collect(Collectors.toList());
    }


    /**
     * Get all predefined buildings.
     *
     * @return the buildings
     */
    public static List<BuildingDescriptor> getAll() {
        return buildings;
    }
}
