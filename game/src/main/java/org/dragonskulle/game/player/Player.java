/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.HighlightSelection;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.game.player.networkData.StatData;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * This is the class which contains all the needed data to play a game
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class Player extends NetworkableComponent implements IOnStart, IFixedUpdate {

    // List of Buildings -- stored & synced in HexagonMap
    private final Map<HexagonTile, Reference<Building>> mOwnedBuildings = new HashMap<>();
    // The map component
    private Reference<HexagonMap> mMapComponent; // This should be synced.  Where who knows!

    private final Map<Integer, Reference<Player>> mPlayersOnline = new TreeMap<>();

    @Getter public SyncInt mTokens = new SyncInt(0);
    @Getter public final SyncVector3 mPlayerColour = new SyncVector3();
    @Getter private HighlightSelection mPlayerHighlightSelection;

    private static final Vector3f[] COLOURS = {
        new Vector3f(0.5f, 1f, 0.05f),
        new Vector3f(0.05f, 1f, 0.86f),
        new Vector3f(0.89f, 0.05f, 1f),
        new Vector3f(0.1f, 0.56f, 0.05f),
        new Vector3f(0.05f, 1f, 0.34f),
        new Vector3f(0.05f, 1f, 0.34f),
        new Vector3f(0.1f, 0.05f, 0.56f),
        new Vector3f(0f, 0f, 0f)
    };

    private final int TOKEN_RATE = 5;
    private final float UPDATE_TIME = 1;
    private float mLastTokenUpdate = 0;

    /** The base constructor for player */
    public Player() {}

    @Override
    protected void onConnectedSyncvars() {
        if (getNetworkObject().isServer()) {
            int id = getNetworkObject().getOwnerId() % COLOURS.length;
            if (id < 0) id += COLOURS.length;
            mPlayerColour.set(COLOURS[id]);
        }
    }

    @Override
    public void onStart() {

        mMapComponent =
                Scene.getActiveScene()
                        .getSingleton(HexagonMap.class)
                        .getReference(HexagonMap.class);

        Vector3fc col = mPlayerColour.get();
        mPlayerHighlightSelection =
                MapEffects.highlightSelectionFromColour(col.x(), col.y(), col.z());

        // mOwnedBuildings.add(capital);
        // TODO Get all Players & add to list
        updateTokens(UPDATE_TIME);
    }

    public HexagonMap getMapComponent() {
        return mMapComponent == null ? null : mMapComponent.get();
    }

    public Player getTileOwner(HexagonTile tile) {
        Building building = tile.getBuilding();

        if (building != null) {
            return building.getOwner();
        }

        for (HexagonTile nearTile : getTilesInRadius(5, tile)) {
            building = nearTile.getBuilding();
            if (building != null) {
                if (building.getTile().equals(tile)) return building.getOwner();
                if (building.getViewableTiles().contains(tile)) return building.getOwner();
            }
        }

        return null;
    }

    /**
     * Add a building to the ones the player owns
     *
     * @param building
     */
    public void addBuilding(Building building) {
        mOwnedBuildings.put(building.getTile(), building.getReference(Building.class));
        // building.getTile().setBuilding(building);
        log.info("Building size" + mOwnedBuildings.size());
        log.info("Added Building " + building.getTile().getQ() + " " + building.getTile().getR());
    }

    public void removeBuilding(Building buildingToRemove) {
        mOwnedBuildings.remove(buildingToRemove.getTile());
    }

    public Reference<Building> getBuilding(HexagonTile tile) {
        return mOwnedBuildings.get(tile);
    }

    public Stream<Reference<Building>> getBuildings() {
        return mOwnedBuildings.values().stream();
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
        if (getNetworkObject().isServer()) {
            mLastTokenUpdate += time;
            // Checks to see how long its been since lastTokenUpdate
            if (mLastTokenUpdate >= UPDATE_TIME) {

                // Add tokens for each building
                mOwnedBuildings.values().stream()
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .forEach(
                                b ->
                                        mTokens.set(
                                                mTokens.get() + b.getTokenGeneration().getValue()));

                mTokens.set(mTokens.get() + TOKEN_RATE);
                mLastTokenUpdate = 0;
                log.info("Tokens at: " + mTokens.get());
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

        if (getNetworkObject().isMine()) Scene.getActiveScene().registerSingleton(this);
    }

    @Override
    protected void onDestroy() {}

    // Selling of buildings is handled below
    @Getter private transient ClientRequest<SellData> mClientSellRequest;

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
    @Getter private transient ClientRequest<AttackData> mClientAttackRequest;

    /**
     * How this component will react to an attack event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(AttackData data) {

        int COST = 5; // 	TODO MOVE TO BUILDING OR ATTACK.  BASICALLY A BETTER PLACE THAN THIS

        // Checks if there is enough tokens for this
        if (mTokens.get() < COST) {
            log.info("Do not have enough for attack");
            return;
        }

        // Get the hexagon tiles
        Building attackingBuilding = data.getAttackingFrom(null);
        Building defenderBuilding = data.getAttackingTo(null);

        if (attackingBuilding == null
                || defenderBuilding == null
                || attackingBuilding.getNetworkObject().getOwnerId()
                        != getNetworkObject().getOwnerId()) {
            log.info("Invalid building selection!");
            return;
        }

        if (!attackingBuilding.isBuildingAttackable(defenderBuilding)) {
            log.info("Player passed a non-attackable building!");
            return;
        }

        // Checks building is correct
        if (defenderBuilding.getOwnerID() == attackingBuilding.getOwnerID()) {
            log.info("ITS YOUR BUILDING DUMMY");
            return;
        }

        // ATTACK!!! (Sorry...)
        boolean won = attackingBuilding.attack(defenderBuilding);
        log.info("Attack is: " + Boolean.toString(won));
        mTokens.set(mTokens.get() - COST);

        // If you've won attack
        if (won) {
            Reference<Player> player = mPlayersOnline.get(defenderBuilding.getOwnerID());

            if (player != null && player.isValid()) {
                player.get().mOwnedBuildings.remove(defenderBuilding.getTile());
            } else {
                log.warning("Player not found!");
            }

            mOwnedBuildings.put(
                    defenderBuilding.getTile(), defenderBuilding.getReference(Building.class));
            defenderBuilding.getNetworkObject().setOwnerId(attackingBuilding.getOwnerID());
        }
        log.info("Done");

        return;
    }

    // Building is handled below
    @Getter private transient ClientRequest<BuildData> mClientBuildRequest;

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
            log.info("Not enough tokens for building");
            return;
        }

        // Gets the actual tile
        HexagonMap map = mMapComponent.get();
        HexagonTile tile = data.getTile(map);

        if (tile.getBuilding() != null) {
            log.info("Building already exists!");
            return;
        }

        log.info("Got the map & tile");
        if (buildingWithinRadius(getTilesInRadius(1, tile))) { // TODO Merge into one function
            log.info("Trying to build too close to another building");
            return;
        }

        // TODO REDO
        // Create a new building.
        NetworkManager networkManager = getNetworkObject().getNetworkManager();

        if (networkManager.getServerManager() == null) {
            log.warning("Server manager is null.");
            return;
        }

        Reference<NetworkObject> obj =
                networkManager
                        .getServerManager()
                        .spawnNetworkObject(
                                getNetworkObject().getOwnerId(),
                                networkManager.findTemplateByName("building"));
        if (obj != null) {
            GameObject buildingGO = obj.get().getGameObject();

            buildingGO.getTransform(TransformHex.class).setPosition(tile.getQ(), tile.getR());

            // Remove the tokens.
            mTokens.set(mTokens.get() - COST);

            // mOwnedBuildings = new ArrayList<Reference<Building>>();
        }
        log.info("Building added");
    }

    public boolean buildingWithinRadius(ArrayList<HexagonTile> tiles) {
        for (HexagonTile tile : tiles) {
            if (mMapComponent.isValid()
                    && mMapComponent.get().getBuilding(tile.getQ(), tile.getR()) != null) {
                return true;
            }
        }
        return false;
    }

    // Upgrading Stats is handled below
    @Getter private transient ClientRequest<StatData> mClientStatRequest;

    /**
     * How this component will react to an upgrade event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(StatData data) {
    	log.info("Increasing stat.");
        HexagonMap map = mMapComponent.get();
        Building building = data.getBuilding(map);
        
        // Used for testing:
        if(building.getAttack().get() + 1 > SyncStat.LEVEL_MAX) {
        	building.getAttack().setLevel(0);
        } else {
        	building.getAttack().increaseLevel();
        }
        
        
        log.info("Suggesting that stats changed.");
        //building.statsChanged();
        
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

    @Override
    public void fixedUpdate(float deltaTime) {

        updateTokens(deltaTime);
    }
}
