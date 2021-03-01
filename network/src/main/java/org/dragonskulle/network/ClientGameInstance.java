/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.Capitol;
import org.dragonskulle.network.components.Networkable;

import java.util.ArrayList;

public class ClientGameInstance {
    private HexagonTile[][] map;

    private final ArrayList<Networkable> networkedComponents = new ArrayList<>();


    public ArrayList<Networkable> getNetworkedComponents() {
        return networkedComponents;
    }


    public void spawnCapitol(Capitol capitol) {
        this.networkedComponents.add(capitol);
    }

    public boolean isSetup() {
        return this.map != null;
    }

    public void spawnMap(HexagonTile[][] spawnedMap) {
        this.map = spawnedMap;
    }
}
