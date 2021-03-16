/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.game.player.networkData.StatData;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.SyncInt;

/**
 * This is the class which contains all the needed data to play a game
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class Player extends NetworkableComponent implements IOnStart {

	// List of Buildings -- stored & synced in HexagonMap
    private List<Reference<Building>>
            mOwnedBuildings; 
    // The map component
    @Getter private Reference<HexagonMap> mMapComponent; // This should be synced.  Where who knows!
    
    
    private final int UNIQUE_ID;
    private static int mNextID; 
    @Getter private SyncInt mTokens = new SyncInt(0);
    private final int TOKEN_RATE = 5;
    private final float UPDATE_TIME = 1;
    private float mLastTokenUpdate = 0;

    /**
     * The base constructor for player
     *
     * @param map the map being used for this game
     * @param capital the capital used by the player
     */
    public Player() { 
        UNIQUE_ID = 5; // TODO need to make this static so unique for each player
    }

    
	@Override
	public void onStart() {
		
		/*mMapComponent = getGameObject.getComponent();
        mOwnedBuildings = new ArrayList<Reference<Building>>();
        mOwnedBuildings.add(capital);
        updateTokens(UPDATE_TIME + 1);*/
	}
    /**
     * Add a building to the ones the player owns
     *
     * @param building
     */
    public void addBuilding(Reference<Building> building) {
        mOwnedBuildings.add(building);
    }

    /**
     * Get a reference to the building at this index
     *
     * @param index The index to get data from
     * @return The building
     */
    public Reference<Building> getBuilding(int index) {
        return mOwnedBuildings.get(index);
    }

    /**
     * The number of buildings the player has
     *
     * @return The number of buildings
     */
    public int numberOfBuildings() {
        return mOwnedBuildings.size();
    }

    /**
     * This method will update the amount of tokens the user has per UPDATE_TIME. Goes through all
     * owned buildings to check if need to update tokens. Should only be ran on the server
     */
    public void updateTokens(float time) {

    	// Checks if server
        if (getNetworkObject() != null && getNetworkObject().isServer()) {
        	
            mLastTokenUpdate += time;
            // Checks to see how long its been since lastTokenUpdate
            if (mLastTokenUpdate > UPDATE_TIME) {

                // Add tokens for each building
                for (Reference<Building> building : mOwnedBuildings) {
                    mTokens.set(mTokens.get() + building.get().getTokenGeneration().getValue());
                }
                // Add final tokens

                mTokens.set(mTokens.get() + TOKEN_RATE);
                mLastTokenUpdate = 0;
            }
        }
    }

    /** We need to initialize requests here, since java does not like to serialize lambdas */
    @Override
    protected void onNetworkInitialize() {
    	
        mClientSellRequest = new ClientRequest<>(new SellData(), this::handleEvent);
        mClientAttackRequest = new ClientRequest<>(new AttackData(), this::handleEvent);
        mClientBuildRequest = new ClientRequest<>(new BuildData(), this::handleEvent);
        mClientStatRequest = new ClientRequest<>(new StatData(), this::handleEvent);
    }

    @Override
    protected void onDestroy() {}

    // Selling of buildings is handled below
    public transient ClientRequest<SellData> mClientSellRequest;

    /**
     * How this component will react to an sell event.
     *
     * @param data sell event being executed on the server.
     */
    public void handleEvent(SellData data) {
        // TODO implement
        // get building
        // verify the sender owns the building
        // remove from owned buildings
        // remove from map
        // reimburse player with tokens
    }

    // attacking of buildings is handled below
    public transient ClientRequest<AttackData> mClientAttackRequest;

    /**
     * How this component will react to an attack event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(AttackData data) {

        int COST = 5; // 	TODO MOVE TO BUILDING OR ATTACK.  BASICALLY A BETTER PLACE THAN THIS

        // Checks if there is enough tokens for this
        if (mTokens.get() < COST) {
            return;
        }

        // Get the hexagon tiles
        HexagonTile attackerTile = data.getAttackingFrom();
        HexagonTile defenderTile = data.getAttacking();

        // Create 2 dummy Buildings to use for checks.
        Building attackingFrom =
                new Building(mMapComponent, new Reference<HexagonTile>(attackerTile));
        Building defender = new Building(mMapComponent, new Reference<HexagonTile>(defenderTile));

        // Checks to see if building is yours and if so get proper one
        Reference<Building> attacker = checkBuildingYours(attackingFrom);

        if (attacker == null) {
            return;
        }

        // Get the proper version of the defending building
        ArrayList<Building> attackableBuildings = attacker.get().getAttackableBuildings();
        Building defending = checkAttackable(defender, attackableBuildings);

        if (defending == null) {
            return;
        }
        // Checks building is correct 
        Reference<Building> isYours = checkBuildingYours(defending);
        if (isYours != null) {
            return;
        }

        // ATTACK!!! (Sorry...)
        attacker.get().attack(defending);
        mTokens.set(mTokens.get() - COST);

        return;
    }

    /**
     * Checks if the building coordinates correspond to a building a player owns
     *
     * @param buildingToCheck The building to check
     * @return true of the player owns it, false if not
     */
    private Reference<Building> checkBuildingYours(Building buildingToCheck) {
    	
    	// Checks the building is yours
        for (Reference<Building> building : mOwnedBuildings) {
        	
            if (building.get().getTile().get().getR() == buildingToCheck.getTile().get().getR()
                    && building.get().getTile().get().getQ()
                            == buildingToCheck.getTile().get().getQ()) {
                return building;
            }
        }

        return null;
    }

    /**
     * Checks if the building coordinates corresponds to a building coordinates in the list
     *
     * @param buildingToCheck The building to check is in the list
     * @param buildingsToCheck The list
     * @return true if in the list false if not
     */
    private Building checkAttackable(
            Building buildingToCheck, ArrayList<Building> buildingsToCheck) {
    	
    	// Checks building is yours
        for (Building building : buildingsToCheck) {
            if (building.getTile().get().getR() == buildingToCheck.getTile().get().getR()
                    && building.getTile().get().getQ() == buildingToCheck.getTile().get().getQ()) {
                return building;
            }
        }

        return null;
    }

    // Building is handled below
    public transient ClientRequest<BuildData> mClientBuildRequest;

    /**
     * How this component will react to a Build event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(BuildData data) {
        // TODO implement
        // get Hexagon to build on
        // Add to the HexagonMap
        // Take tokens off

        // TODO: Move to Building.
        int COST = 5;

        if (mTokens.get() < COST) {
            return;
        }

        // Remove the tokens.
        mTokens.set(mTokens.get() - COST);

        // Contains the coordinates:
        HexagonTile tileCoordinates = data.getHexTile();

        HexagonMap map = mMapComponent.get();
        HexagonTile tile = map.getTile(tileCoordinates.getQ(), tileCoordinates.getR());

        if (buildingWithinRadius(getTilesInRadius(1, tile))) {
            return;
        }

        // Create a new building.
        Building building = new Building(mMapComponent, new Reference<HexagonTile>(tile));

        // Store the building.
        map.storeBuilding(building, tile.getQ(), tile.getR());
    }

    public boolean buildingWithinRadius(ArrayList<HexagonTile> tiles) {
        for (HexagonTile tile : tiles) {
            if (mMapComponent.get().getBuilding(tile.getQ(), tile.getR()) != null) {
                return true;
            }
        }
        return false;
    }

    // Upgrading Stats is handled below
    public transient ClientRequest<StatData> mClientStatRequest;

    /**
     * How this component will react to an upgrade event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(StatData data) {
        // TODO implement
        // Get Building
        // Get Stat
        // Upgrade

    }

    /**
     * This will return all hex tiles within a radius except the one in the tile
     *
     * @param radius The radius to check
     * @param tile the tile to check
     * @return A list of tiles
     */
    public ArrayList<HexagonTile> getTilesInRadius(
            int radius,
            HexagonTile
                    tile) { // TODO Repeated code from building need to move in more sensible place
        ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();

        // Attempt to get the current HexagonTile and HexagonMap.
        HexagonMap map = mMapComponent.get();
        if (tile == null || map == null) return tiles;

        // Get the current q and r coordinates.
        int qCentre = tile.getQ();
        int rCentre = tile.getR();

        for (int rOffset = -radius; rOffset <= radius; rOffset++) {
            for (int qOffset = -radius; qOffset <= radius; qOffset++) {
                // Only get tiles whose s coordinates are within the desired range.
                int sOffset = -qOffset - rOffset;

                // Do not include tiles outside of the radius.
                if (sOffset > radius || sOffset < -radius) continue;
                // Do not include the building's HexagonTile.
                if (qOffset == 0 && rOffset == 0) continue;

                // log.info(String.format("qOffset = %d, rOffset = %d, s = %d ", qOffset, rOffset,
                // s));

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = map.getTile(qCentre + qOffset, rCentre + rOffset);
                if (selectedTile == null) continue;

                // Add the tile to the list.
                tiles.add(selectedTile);
            }
        }

        // log.info("Number of tiles in range: " + tiles.size());

        return tiles;
    }
}
