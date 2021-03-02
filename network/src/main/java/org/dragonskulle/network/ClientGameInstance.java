/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.Networkable;

public class ClientGameInstance {
    private HexagonTile[][] map;

    private final ArrayList<Networkable> networkedComponents = new ArrayList<>();

    public ArrayList<Networkable> getNetworkedComponents() {
        return networkedComponents;
    }

    public boolean isSetup() {
        return this.map != null;
    }

    public void spawnCapital(Capital capital) {
        this.networkedComponents.add(capital);
    }

    public void spawnMap(HexagonTile[][] spawnedMap) {
        this.map = spawnedMap;
    }

    public void updateNetworkable(byte[] payload) {
        // 36 bytes will be allocated for the id
        String idToUpdate = Networkable.getIdFromBytes(payload);
        Networkable networkableToUpdate = getNetworkable(idToUpdate);
        if (networkableToUpdate != null) {
            System.out.println("found networkable, should update");
            try {
                networkableToUpdate.updateFromBytes(payload);
                //                System.out.println("if i got here i actually updated the correct
                // game component");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Networkable getNetworkable(String id) {
        for (Networkable networkedComponent : this.networkedComponents) {
            if (networkedComponent.getId().equals(id)) {
                return networkedComponent;
            }
        }
        return null;
    }

    public void printNetworkable(String id) {
        System.out.println(Objects.requireNonNull(getNetworkable(id)).toString());
    }
}
