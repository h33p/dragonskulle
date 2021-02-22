/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.util.stream.IntStream;

class QueueFamilyIndices {
    Integer graphicsFamily;
    Integer presentFamily;

    boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }

    int[] uniqueFamilies() {
        return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
    }
}
