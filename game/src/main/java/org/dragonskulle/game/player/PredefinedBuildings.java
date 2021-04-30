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
            new BuildingDescriptor(1, 1, 1, 25, 10, "ui/1_stars.png", "Camp");
    /** The predefined buildings. */
    private static final List<BuildingDescriptor> buildings =
            new ArrayList<>(
                    Arrays.asList(
                            BASE,
						    new BuildingDescriptor(3, 3, 3, 25, 20, "ui/2_stars.png", "Town"),
						    new BuildingDescriptor(6, 6, 6, 25, 30, "ui/3_stars.png", "City"),
                            
                            new BuildingDescriptor(5, 3, 1, 125, 25, "ui/5_stars.png", "Barracks"),
						    new BuildingDescriptor(3, 5, 1, 125, 25, "ui/5_stars.png", "Castle"),
						    new BuildingDescriptor(1, 3, 5, 125, 25, "ui/5_stars.png", "Merchants"),
                    
						    new BuildingDescriptor(10, 8, 8, 250, 50, "ui/9_stars.png", "Military Complex"),
						    new BuildingDescriptor(8, 10, 8, 250, 50, "ui/9_stars.png", "Fortress"),
						    new BuildingDescriptor(8, 8, 10, 250, 50, "ui/9_stars.png", "Guild")
            		));
    
    

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
