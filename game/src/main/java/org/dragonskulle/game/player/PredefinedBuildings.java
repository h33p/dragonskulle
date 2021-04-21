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
    /** The predefined buildings. */
    private static final List<BuildingDescriptor> buildings =
            new ArrayList<>(
                    Arrays.asList(
                            new BuildingDescriptor(1, 1, 1, 1, 1, 10, 2, "ui/1_stars.png", "Oscar"),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 30, 2, "ui/2_stars.png", "Auri"),
                            new BuildingDescriptor(
                                    1, 3, 10, 2, 3, 31, 2, "ui/3_stars.png", "Harry"),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 32, 2, "ui/4_stars.png", "Auri"),
                            new BuildingDescriptor(
                                    1, 3, 10, 2, 3, 33, 2, "ui/5_stars.png", "Leela"),
                            new BuildingDescriptor(
                                    1, 1, 100, 1, 1, 25, 2, "ui/6_stars.png", "Craig")));

    private static final BuildingDescriptor BASE =
            new BuildingDescriptor(1, 1, 1, 1, 1, 1, 1, "ui/7_stars.png", "Base");

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
