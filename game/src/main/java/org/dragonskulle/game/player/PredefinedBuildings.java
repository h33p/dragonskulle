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
                            new BuildingDescriptor(1, 1, 1, 1, 1, 10, 2, null, "Oscar"),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 30, 2, null, "Auri"),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 31, 2, null, "Harry"),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 32, 2, null, "Auri"),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 33, 2, null, "Leela"),
                            new BuildingDescriptor(1, 1, 100, 1, 1, 25, 2, null, "Craig")));
    private static final BuildingDescriptor BASE = new BuildingDescriptor(1, 1, 1, 1, 1, 1, 1, null, "Base");

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

    public static BuildingDescriptor get(int i) {
        if (i <= buildings.size()) {
            return buildings.get(i);

        }
        return PredefinedBuildings.BASE;
    }

    public static int getIndex(BuildingDescriptor mSelectedBuildingDescriptor) {
        return buildings.indexOf(mSelectedBuildingDescriptor);
    }
}
