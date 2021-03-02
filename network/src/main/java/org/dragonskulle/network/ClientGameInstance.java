/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * The type Client game instance, used to store all game data. This will be replaced by the game
 * engines data.
 */
public class ClientGameInstance {
    /** The Map. */
    private HexagonTile[][] map;

    /** The Networked components. */
    private final ArrayList<NetworkableComponent> networkedComponents = new ArrayList<>();

    /**
     * True if a capital has been spawned.
     */
    private Boolean hasCapital = false;

    /**
     * Gets networked components.
     *
     * @return the networked components
     */
    public ArrayList<NetworkableComponent> getNetworkedComponents() {
        return networkedComponents;
    }

    /**
     * If the game instance is setup it will return true.
     *
     * @return the boolean
     */
    public boolean isSetup() {
        return this.map != null;
    }

    /**
     * Spawn a capital locally.
     *
     * @param capital the capital
     */
    public void spawnCapital(Capital capital) {
        this.networkedComponents.add(capital);
        this.hasCapital = true;
    }

    /**
     * Spawn a map locally.
     *
     * @param spawnedMap the spawned map
     */
    public void spawnMap(HexagonTile[][] spawnedMap) {
        this.map = spawnedMap;
    }

    /**
     * Update networkable from server message.
     *
     * @param payload the payload
     */
    public void updateNetworkable(byte[] payload) {
        // 36 bytes will be allocated for the id
        String idToUpdate = NetworkableComponent.getIdFromBytes(payload);
        NetworkableComponent networkableComponentToUpdate = getNetworkable(idToUpdate);
        if (networkableComponentToUpdate != null) {
            System.out.println("found networkable, should update");
            try {
                networkableComponentToUpdate.updateFromBytes(payload);
                //                System.out.println("if i got here i actually updated the correct
                // game component");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets networkable by the id of the component.
     *
     * @param id the id
     * @return the networkable
     */
    private NetworkableComponent getNetworkable(String id) {
        for (NetworkableComponent networkedComponent : this.networkedComponents) {
            if (networkedComponent.getId().equals(id)) {
                return networkedComponent;
            }
        }
        return null;
    }

    /**
     * Print networkable.
     *
     * @param id the id of the component
     */
    public void printNetworkable(String id) {
        System.out.println(Objects.requireNonNull(getNetworkable(id)).toString());
    }

    /**
     * Function for testing
     * @return
     */
    public boolean hasSpawnedMap() {
        return this.map != null;
    }

    public Boolean hasSpawnedCapital() {
        return this.hasCapital;
    }
}
