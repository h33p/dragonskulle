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
 * by the game engines data.
 */
public class ClientGameInstance {
    /**
     * The Map.
     */
    private byte[] mMap;

    /**
     * The Networked components.
     */
    private final ArrayList<NetworkableComponent> mNetworkedComponents = new ArrayList<>();

    /**
     * The Networked objects.
     */
    private final ArrayList<NetworkObject> mNetworkedObjects = new ArrayList<>();
    /**
     * True if a capital has been spawned.
     */
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
        NetworkableComponent found = null;
        for (NetworkObject nob : mNetworkedObjects) {
            NetworkableComponent nc = nob.findComponent(id);
            if (nc != null) {
                found = nc;
                break;
            }
        }
        return found;
    }

    /**
     * Gets a network object by id.
     *
     * @param networkObjectId the Id of the object
     * @return the network object
     */
    public NetworkObject getNetworkObject(int networkObjectId) {
        System.out.println("Should find network object by id");
        System.out.println("looking for " + networkObjectId);
        System.out.println(mNetworkedComponents);
        return mNetworkedObjects.stream().filter(e -> e.getId() == networkObjectId).findFirst().orElse(null);
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
    public int spawnCapital(int networkObjectId, Capital capital) {
        NetworkObject nob = this.mNetworkedObjects.stream().filter(e -> e.getId() == networkObjectId).findFirst().orElse(null);
        if (nob != null) {
            nob.addChild(capital);
            this.mHasCapital = true;
        } else {
            nob = new NetworkObject(networkObjectId);
            nob.addChild(capital);
            this.mNetworkedObjects.add(nob);
            this.mHasCapital = true;
            System.out.print("Created new nob with id " + networkObjectId);
        }
        return capital.getId();
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
            System.out.println("found network object, should update");
            try {
                System.out.println("BEFORE UPDATE");
                System.out.println(networkObjectToUpdate.toString());
                networkObjectToUpdate.updateFromBytes(payload);
                System.out.println("AFTER UPDATE");
                System.out.println(networkObjectToUpdate.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("didnt find network object from id");
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
