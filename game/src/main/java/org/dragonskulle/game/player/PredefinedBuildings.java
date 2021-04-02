/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** @author Oscar L */
public class PredefinedBuildings {
    private static final List<BuildingDescriptor> buildings =
            new ArrayList<>(
                    Arrays.asList(
                            new BuildingDescriptor(1, 1, 1, 1, 1, 10, 2),
                            new BuildingDescriptor(1, 3, 10, 2, 3, 30, 2),
                            new BuildingDescriptor(1, 1, 100, 1, 1, 25, 2)));

    public static List<BuildingDescriptor> getPlaceable(int currentTokens) {
        return buildings.stream()
                .filter(b -> b.getCost() <= currentTokens)
                .collect(Collectors.toList());
    }
}
