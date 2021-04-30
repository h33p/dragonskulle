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
    /** The Base building. */
    public static final BuildingDescriptor BASE =
            new BuildingDescriptor(1, 1, 1, 1, 1, "ui/0_stars.png", "Stock");
    /** The predefined buildings. */
    private static final List<BuildingDescriptor> buildings =
            new ArrayList<>(
                    Arrays.asList(
                            BASE,
                            new BuildingDescriptor(1, 1, 1, 100, 200, "ui/1_stars.png", "Oscar"),
                            new BuildingDescriptor(2, 3, 2, 30, 999, "ui/2_stars.png", "Auri"),
                            new BuildingDescriptor(5, 3, 4, 31, 2, "ui/3_stars.png", "Harry"),
                            new BuildingDescriptor(8, 3, 6, 32, 2, "ui/4_stars.png", "Auri"),
                            new BuildingDescriptor(10, 3, 8, 33, 2, "ui/5_stars.png", "Leela"),
                            new BuildingDescriptor(4, 1, 10, 25, 2, "ui/6_stars.png", "Craig"),
						    new BuildingDescriptor(4, 1, 10, 25, 2, "ui/6_stars.png", "Craig"),
						    new BuildingDescriptor(4, 1, 10, 25, 2, "ui/6_stars.png", "Craig"),
						    new BuildingDescriptor(4, 1, 10, 25, 2, "ui/9_stars.png", "Craig")));

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
