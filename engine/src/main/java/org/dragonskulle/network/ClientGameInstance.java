/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * @author Oscar L The type Client game instance, used to store all game data. This will be replaced
 *     by the game engines data.
 */
public class ClientGameInstance {
    /** The Map. */
    private byte[] mMap;

    /** The Networked components. */
    private final ArrayList<NetworkableComponent> mNetworkedComponents = new ArrayList<>();

    /** The Networked objects. */
    private final ArrayList<NetworkObject> mNetworkedObjects = new ArrayList<>();
    /** True if a capital has been spawned. */
    private Boolean mHasCapital = false;

    /**
     * Gets networked components.
     *
     * @return the networked components
     */
    public ArrayList<NetworkableComponent> getNetworkedComponent() {
        return mNetworkedComponents;
    }

    /**
     * Gets networke objects.
     *
     * @return the network objects
     */
    public ArrayList<NetworkObject> getNetworkObjects() {
        return mNetworkedObjects;
    }

    /**
     * Gets a networked component by id.
     *
     * @param id the Id of the component
     * @return the networked components
     */
    public NetworkableComponent getNetworkedComponent(int id) {
        return mNetworkedComponents.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    /**
     * Gets a network object by id.
     *
     * @param id the Id of the object
     * @return the network object
     */
    public NetworkObject getNetworkObject(int id) {
        return mNetworkedObjects.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    /**
     * If the game instance is setup it will return true.
     *
     * @return the boolean
     */
    public boolean isSetup() {
        return this.mMap != null;
    }

    /**
     * Spawn a capital locally.
     *
     * @param capital the capital
     */
    public void spawnCapital(Capital capital) {
        this.mNetworkedComponents.add(capital);
        this.mHasCapital = true;
    }

    /**
     * Spawn a map locally.
     *
     * @param spawnedMap the spawned map
     */
    public void spawnMap(byte[] spawnedMap) {
        this.mMap = spawnedMap;
    }

    /**
     * Update networkable from server message.
     *
     * @param payload the payload
     */
    public void updateNetworkObject(byte[] payload) {
        // 4 bytes will be allocated for the id
        int idToUpdate = NetworkObject.getIdFromBytes(payload);
        NetworkObject networkObjectToUpdate = getNetworkObject(idToUpdate);
        if (networkObjectToUpdate != null) {
            System.out.println("found networkab object, should update");
            try {
                networkObjectToUpdate.updateFromBytes(payload);
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
    private NetworkableComponent getNetworkable(int id) {
        for (NetworkableComponent networkedComponent : this.mNetworkedComponents) {
            if (networkedComponent.getId() == id) {
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
    public void printNetworkable(int id) {
        System.out.println(Objects.requireNonNull(getNetworkable(id)).toString());
    }

    /**
     * Function for testing
     *
     * @return Returns the status of the spawned map
     */
    public boolean hasSpawnedMap() {
        return this.mMap != null;
    }

    public Boolean hasSpawnedCapital() {
        return this.mHasCapital;
    }
}
